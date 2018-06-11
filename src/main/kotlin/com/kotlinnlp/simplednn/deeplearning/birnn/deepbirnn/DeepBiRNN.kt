/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.simplednn.deeplearning.birnn.deepbirnn

import com.kotlinnlp.simplednn.deeplearning.birnn.BiRNN
import com.kotlinnlp.utils.Serializer
import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable

/**
 * The DeepBiRNN.
 *
 * A deep bidirectional RNN (or k-layer BiRNN) is composed of k BiRNNs that feed into each other: the output of the i-th
 * BIRNN becomes the input of the (i+1)-th BiRNN. Stacking BiRNNs in this way has been empirically shown to be effective
 * (Irsoy and Cardie, 2014).
 *
 * The output size of each BiRNN must be equal to the input size of the following one.
 *
 * @property layers the list of BiRNNs
 */
class DeepBiRNN(val layers: List<BiRNN>) : Serializable {

  companion object {

    /**
     * Private val used to serialize the class (needed from Serializable)
     */
    @Suppress("unused")
    private const val serialVersionUID: Long = 1L

    /**
     * Read a [DeepBiRNN] (serialized) from an input stream and decode it.
     *
     * @param inputStream the [InputStream] from which to read the serialized [DeepBiRNN]
     *
     * @return the [DeepBiRNN] read from [inputStream] and decoded
     */
    fun load(inputStream: InputStream): DeepBiRNN = Serializer.deserialize(inputStream)
  }

  /**
   * The model parameters.
   */
  val model = DeepBiRNNParameters(paramsPerBiRNN = this.layers.map { it.model })

  /**
   * The size of the output layer (of the last BiRNN)
   */
  val outputSize: Int = this.layers.last().outputSize

  /**
   * Check the compatibility of the BiRNNs.
   */
  init {
    require(this.layers.isNotEmpty()) { "The list of BiRNNs cannot be empty" }
  }

  /**
   * Serialize this [DeepBiRNN] and write it to an output stream.
   *
   * @param outputStream the [OutputStream] in which to write this serialized [DeepBiRNN]
   */
  fun dump(outputStream: OutputStream) = Serializer.serialize(this, outputStream)
}
