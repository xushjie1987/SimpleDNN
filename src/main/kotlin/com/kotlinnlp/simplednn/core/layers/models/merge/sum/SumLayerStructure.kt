/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.simplednn.core.layers.models.merge.sum

import com.kotlinnlp.simplednn.core.arrays.AugmentedArray
import com.kotlinnlp.simplednn.core.layers.models.merge.MergeLayer
import com.kotlinnlp.simplednn.simplemath.ndarray.NDArray
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray

/**
 * The Sum Layer Structure.
 *
 * @property inputArrays the input arrays of the layer
 * @property params the parameters which connect the input to the output
 * @property id an identification number useful to track a specific [SumLayerStructure]
 */
class SumLayerStructure<InputNDArrayType : NDArray<InputNDArrayType>>(
  inputArrays: List<AugmentedArray<InputNDArrayType>>,
  outputArray: AugmentedArray<DenseNDArray>,
  override val params: SumLayerParameters,
  id: Int = 0
) : MergeLayer<InputNDArrayType>(
  inputArrays = inputArrays,
  outputArray = outputArray,
  params = params,
  activationFunction = null,
  dropout = 0.0,
  id = id) {

  init { this.checkInputSize() }

  /**
   * The helper which execute the forward.
   */
  override val forwardHelper = SumForwardHelper(layer = this)

  /**
   * The helper which execute the backward.
   */
  override val backwardHelper = SumBackwardHelper(layer = this)

  /**
   * The helper which calculates the relevance.
   */
  override val relevanceHelper = SumRelevanceHelper(layer = this)

  /**
   * @return the [SumLayerParameters] used to store errors
   */
  override fun parametersErrorsFactory() = SumLayerParameters(
    inputSize = this.params.inputSize,
    nInputs = this.params.nInputs)
}
