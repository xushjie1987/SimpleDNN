/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.simplednn.deeplearning.competitivelearning

import com.kotlinnlp.simplednn.core.functionalities.losses.MSECalculator
import com.kotlinnlp.simplednn.core.neuralprocessor.feedforward.FeedforwardNeuralProcessor
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray
import com.kotlinnlp.simplednn.simplemath.similarity

/**
 * The Competitive Learning Network.
 *
 * @property model the model of this network
 */
class CLNetwork(val model: CLNetworkModel) {

  /**
   * The scores of the last prediction.
   */
  val outputScores: Map<Int, Double> get() = this.mutableScores

  /**
   * The mutable map which contains the scores of the last prediction.
   */
  private val mutableScores = mutableMapOf<Int, Double>()

  /**
   * The map that associates each class to a feed-forward processor.
   */
  private val processors: Map<Int, FeedforwardNeuralProcessor<DenseNDArray>> =
    this.model.classes.associate { it to FeedforwardNeuralProcessor<DenseNDArray>(this.model.networks.getValue(it)) }

  /**
   * The loss calculator used to calculate the distance between the input and its reconstruction
   */
  private val lossCalculator = MSECalculator()

  /**
   * The processor for which a backward was called the last time.
   */
  private lateinit var lastBackwardProcessor: FeedforwardNeuralProcessor<DenseNDArray>

  /**
   * Predict.
   *
   * @param inputArray the input array
   *
   * @return the highest scoring predicted class
   */
  fun predict(inputArray: DenseNDArray): Int {

    return this.processors.maxBy { (key, processor) ->

      val score: Double = similarity(inputArray, processor.forward(inputArray))

      this.mutableScores[key] = score

      score
    }!!
      .key
  }

  /**
   * Learn.
   *
   * @param inputArray the input array
   * @param classId the class to which the [inputArray] belongs
   *
   * @return
   */
  fun learn(inputArray: DenseNDArray, classId: Int): Double {

    this.lastBackwardProcessor = checkNotNull(this.processors[classId]) {
      "Unknown class: $classId"
    }

    val reconstructedArray: DenseNDArray = this.lastBackwardProcessor.forward(inputArray)

    val errors: DenseNDArray = this.lossCalculator.calculateErrors(
      output = reconstructedArray,
      outputGold = inputArray)

    this.lastBackwardProcessor.backward(errors)

    return this.lossCalculator.calculateLoss(
      output = reconstructedArray,
      outputGold = inputArray).avg()
  }

  /**
   * @param copy a Boolean indicating whether the returned errors must be a copy or a reference
   *
   * @return the errors of the input
   */
  fun getInputErrors(copy: Boolean = true): DenseNDArray = this.lastBackwardProcessor.getInputErrors(copy = copy)

  /**
   * @param copy a Boolean indicating whether the returned errors must be a copy or a reference
   *
   * @return the errors of the neural parameters
   */
  fun getParamsErrors(copy: Boolean) = CLNetworkParameters(
    networksParams = this.processors.map { it.value.getParamsErrors(copy = copy) }
  )
}