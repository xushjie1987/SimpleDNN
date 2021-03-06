/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.simplednn.deeplearning.attention.pointernetwork

import com.kotlinnlp.simplednn.core.functionalities.initializers.GlorotInitializer
import com.kotlinnlp.simplednn.core.functionalities.initializers.Initializer
import com.kotlinnlp.simplednn.core.attention.AttentionParameters
import com.kotlinnlp.simplednn.core.layers.LayerInterface
import com.kotlinnlp.simplednn.core.neuralnetwork.NeuralNetwork
import com.kotlinnlp.simplednn.deeplearning.birnn.mergeconfig.MergeConfiguration
import com.kotlinnlp.simplednn.deeplearning.birnn.mergeconfig.OpenOutputMerge
import java.io.Serializable


/**
 * The model of the [PointerNetworkProcessor].
 *
 * @property inputSize the size of the elements of the input sequence
 * @property vectorSize the size of the vector that modulates a content-based attention mechanism over the input sequence
 * @param mergeConfig the configuration of the merge network
 * @param weightsInitializer the initializer of the weights (zeros if null, default: Glorot)
 * @param biasesInitializer the initializer of the biases (zeros if null, default: null)
 */
class PointerNetworkModel(
  val inputSize: Int,
  val vectorSize: Int,
  mergeConfig: MergeConfiguration,
  weightsInitializer: Initializer? = GlorotInitializer(),
  biasesInitializer: Initializer? = null
) : Serializable {

  companion object {

    /**
     * Private val used to serialize the class (needed from Serializable).
     */
    @Suppress("unused")
    private const val serialVersionUID: Long = 1L
  }

  /**
   * The merge network used to create the attention arrays of the [attentionParams].
   */
  val mergeNetwork = NeuralNetwork(
    LayerInterface(
      sizes = listOf(this.inputSize, this.vectorSize)
    ),
    LayerInterface(
      // 'size' not used for merge layers with fixed output (e.g. concat, avg, sum, product)
      size = (mergeConfig as? OpenOutputMerge)?.outputSize ?: -1,
      activationFunction = (mergeConfig as? OpenOutputMerge)?.activationFunction,
      connectionType = mergeConfig.type
    ),
    weightsInitializer = weightsInitializer,
    biasesInitializer = biasesInitializer)

  /**
   * The parameters of the attention mechanism.
   */
  val attentionParams = AttentionParameters(
    attentionSize = this.mergeNetwork.outputSize,
    initializer = weightsInitializer)

  /**
   * The structure containing all the parameters of this model.
   */
  val params = PointerNetworkParameters(
    mergeParams = this.mergeNetwork.model,
    attentionParams = this.attentionParams)
}
