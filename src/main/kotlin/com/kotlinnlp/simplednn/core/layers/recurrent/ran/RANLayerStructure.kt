/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.simplednn.core.layers.recurrent.ran

import com.kotlinnlp.simplednn.core.arrays.AugmentedArray
import com.kotlinnlp.simplednn.core.functionalities.activations.ActivationFunction
import com.kotlinnlp.simplednn.core.functionalities.activations.Sigmoid
import com.kotlinnlp.simplednn.core.layers.*
import com.kotlinnlp.simplednn.core.layers.recurrent.*
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray
import com.kotlinnlp.simplednn.simplemath.ndarray.NDArray

/**
 * The RAN Layer Structure.
 *
 * @property inputArray the input array of the layer
 * @property outputArray the output array of the layer
 * @property params the parameters which connect the input to the output
 * @property layerContextWindow the context window used for the forward and the backward
 * @property activationFunction the activation function of the layer
 * @property dropout the probability of dropout (default 0.0).
 *                   If applying it, the usual value is 0.5 (better 0.25 if it's the first layer).
 */
class RANLayerStructure<InputNDArrayType : NDArray<InputNDArrayType>>(
  inputArray: AugmentedArray<InputNDArrayType>,
  outputArray: AugmentedArray<DenseNDArray>,
  params: LayerParameters<*>,
  layerContextWindow: LayerContextWindow,
  activationFunction: ActivationFunction? = null,
  dropout: Double = 0.0
) : GatedRecurrentLayerStructure<InputNDArrayType>(
  inputArray = inputArray,
  outputArray = outputArray,
  params = params,
  layerContextWindow = layerContextWindow,
  activationFunction = activationFunction,
  dropout = dropout) {

  /**
   *
   */
  val candidate = LayerUnit<InputNDArrayType>(outputArray.size)

  /**
   *
   */
  val inputGate = RecurrentLayerUnit<InputNDArrayType>(outputArray.size)

  /**
   *
   */
  val forgetGate = RecurrentLayerUnit<InputNDArrayType>(outputArray.size)

  /**
   * The helper which executes the forward
   */
  override val forwardHelper = RANForwardHelper(layer = this)

  /**
   * The helper which executes the backward
   */
  override val backwardHelper = RANBackwardHelper(layer = this)

  /**
   * The helper which calculates the relevance
   */
  override val relevanceHelper = RANRelevanceHelper(layer = this)

  /**
   * Initialization: set the activation function of the gates
   */
  init {

    this.inputGate.setActivation(Sigmoid())
    this.forgetGate.setActivation(Sigmoid())

    if (this.activationFunction != null) {
      this.outputArray.setActivation(this.activationFunction)
    }
  }

  /**
   * Set the initial hidden array.
   * This method should be used when this layer is used as initial hidden state in a recurrent neural network.
   *
   * @param array the initial hidden array
   */
  override fun setInitHidden(array: DenseNDArray) {
    TODO("not implemented")
  }

  /**
   * Get the errors of the initial hidden array.
   * This method should be used only if this layer is used as initial hidden state in a recurrent neural network.
   *
   * @return the errors of the initial hidden array
   */
  override fun getInitHiddenErrors(): DenseNDArray {
    TODO("not implemented")
  }
}
