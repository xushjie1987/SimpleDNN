/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.simplednn.deeplearning.birnn.deepbirnn

import com.kotlinnlp.simplednn.deeplearning.birnn.BiRNN
import com.kotlinnlp.simplednn.deeplearning.birnn.BiRNNEncoder
import com.kotlinnlp.simplednn.simplemath.ndarray.NDArray
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray

/**
 * Deep Bidirectional Recursive Neural Network Encoder
 *
 * For convenience, this class exposes methods as if there was a single [BiRNN].
 * In this way, it is possible to use a [BiRNNEncoder] and a [DeepBiRNNEncoder] almost interchangeably.
 *
 * @property network the [DeepBiRNN] of this encoder
 */
class DeepBiRNNEncoder<InputNDArrayType: NDArray<InputNDArrayType>>(val network: DeepBiRNN) {

  /**
   * List of encoders for all the stacked [BiRNN] layers.
   */
  private val encoders = this.network.levels.mapIndexed { i, biRNN ->
    if (i == 0)
      BiRNNEncoder<InputNDArrayType>(biRNN)
    else
      BiRNNEncoder<DenseNDArray>(biRNN)
  }

  /**
   * Encode the [sequence].
   *
   * @param sequence the sequence to encode
   * @param useDropout whether to apply the dropout
   *
   * @return the encoded sequence
   */
  @Suppress("UNCHECKED_CAST")
  fun encode(sequence: List<InputNDArrayType>, useDropout: Boolean = false): List<DenseNDArray> {

    var output: List<DenseNDArray>

    output = (this.encoders[0] as BiRNNEncoder<InputNDArrayType>).encode(sequence, useDropout = useDropout)

    for (i in 1 until this.encoders.size) {
      output = (this.encoders[i] as BiRNNEncoder<DenseNDArray>).encode(output, useDropout = useDropout)
    }

    return output
  }

  /**
   * Propagate the errors of the entire sequence.
   *
   * @param outputErrorsSequence the errors to propagate
   * @param propagateToInput whether to propagate the output errors to the input or not
   */
  fun backward(outputErrorsSequence: List<DenseNDArray>, propagateToInput: Boolean) {

    var errors: List<DenseNDArray> = outputErrorsSequence

    for ((i, encoder) in this.encoders.withIndex().reversed()) {
      encoder.backward(errors, propagateToInput = if (i == 0) propagateToInput else true)
      errors = encoder.getInputSequenceErrors(copy = false)
    }
  }

  /**
   * @param copy a Boolean indicating whether the returned errors must be a copy or a reference
   *
   * @return the errors of the input sequence
   */
  fun getInputSequenceErrors(copy: Boolean = true): List<DenseNDArray> =
    this.encoders.first().getInputSequenceErrors(copy = copy)

  /**
   * @param copy a Boolean indicating whether the returned errors must be a copy or a reference
   *
   * @return the errors of the DeepBiRNN parameters
   */
  fun getParamsErrors(copy: Boolean = true) = DeepBiRNNParameters(
    paramsPerBiRNN = this.encoders.map { it.getParamsErrors(copy = copy) }
  )
}
