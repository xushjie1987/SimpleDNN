/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package layers.structure.contextwindows

import com.kotlinnlp.simplednn.core.functionalities.activations.Tanh
import com.kotlinnlp.simplednn.core.arrays.AugmentedArray
import com.kotlinnlp.simplednn.core.layers.recurrent.LayerContextWindow
import com.kotlinnlp.simplednn.core.layers.recurrent.gru.GRULayerParameters
import com.kotlinnlp.simplednn.core.layers.recurrent.gru.GRULayerStructure
import com.kotlinnlp.simplednn.simplemath.ndarray.DenseNDArray
import com.kotlinnlp.simplednn.simplemath.ndarray.DenseNDArrayFactory

/**
 *
 */
sealed class GRULayerContextWindow: LayerContextWindow {

  /**
   *
   */
  class Empty: GRULayerContextWindow() {

    override fun getPrevStateLayer() = null

    override fun getNextStateLayer() = null
  }

  /**
   *
   */
  class Back: GRULayerContextWindow() {

    override fun getPrevStateLayer(): GRULayerStructure<DenseNDArray> = buildPrevStateLayer()

    override fun getNextStateLayer() = null
  }

  /**
   *
   */
  class Front: GRULayerContextWindow() {

    override fun getPrevStateLayer() = null

    override fun getNextStateLayer(): GRULayerStructure<DenseNDArray> = buildNextStateLayer()
  }

  /**
   *
   */
  class Bilateral: GRULayerContextWindow() {

    override fun getPrevStateLayer(): GRULayerStructure<DenseNDArray> = buildPrevStateLayer()

    override fun getNextStateLayer(): GRULayerStructure<DenseNDArray> = buildNextStateLayer()
  }
}

/**
 *
 */
private fun buildPrevStateLayer(): GRULayerStructure<DenseNDArray> {

  val outputArray = AugmentedArray(DenseNDArrayFactory.arrayOf(doubleArrayOf(-0.2, 0.2, -0.3, -0.9, -0.8)))
  outputArray.activate()

  return GRULayerStructure(
    inputArray = AugmentedArray<DenseNDArray>(size = 4),
    outputArray = outputArray,
    params = GRULayerParameters(inputSize = 4, outputSize = 5),
    activationFunction = Tanh(),
    layerContextWindow = GRULayerContextWindow.Empty()
  )
}

/**
 *
 */
private fun buildNextStateLayer(): GRULayerStructure<DenseNDArray> {

  val outputArray: AugmentedArray<DenseNDArray> = AugmentedArray(size = 5)
  outputArray.assignErrors(DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.1, -0.5, 0.7, 0.2)))

  val layer = GRULayerStructure(
    inputArray = AugmentedArray<DenseNDArray>(size = 4),
    outputArray = outputArray,
    params = GRULayerParameters(inputSize = 4, outputSize = 5),
    activationFunction = Tanh(),
    layerContextWindow = GRULayerContextWindow.Empty())

  layer.resetGate.assignValues(DenseNDArrayFactory.arrayOf(doubleArrayOf(0.8, 1.0, -0.8, 0.0, 0.1)))
  layer.resetGate.assignErrors(DenseNDArrayFactory.arrayOf(doubleArrayOf(0.7, -0.3, -0.2, 0.3, 0.6)))
  layer.partitionGate.assignErrors(DenseNDArrayFactory.arrayOf(doubleArrayOf(0.0, 0.9, 0.2, -0.5, 1.0)))
  layer.partitionGate.assignValues(DenseNDArrayFactory.arrayOf(doubleArrayOf(-0.2, -0.1, 0.6, -0.8, 0.5)))
  layer.candidate.assignErrors(DenseNDArrayFactory.arrayOf(doubleArrayOf(-0.4, 0.6, -0.1, 0.3, 0.0)))

  return layer
}
