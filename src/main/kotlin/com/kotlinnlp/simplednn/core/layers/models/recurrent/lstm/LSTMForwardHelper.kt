/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.simplednn.core.layers.models.recurrent.lstm

import com.kotlinnlp.simplednn.core.layers.helpers.ForwardHelper
import com.kotlinnlp.simplednn.core.layers.LayerParameters
import com.kotlinnlp.simplednn.core.layers.LayerStructure
import com.kotlinnlp.simplednn.simplemath.ndarray.NDArray
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray

/**
 * The helper which executes the forward on a [layer].
 *
 * @property layer the [LSTMLayerStructure] in which the forward is executed
 */
class LSTMForwardHelper<InputNDArrayType : NDArray<InputNDArrayType>>(
  override val layer: LSTMLayerStructure<InputNDArrayType>
) : ForwardHelper<InputNDArrayType>(layer) {

  /**
   * Forward the input to the output combining it with the parameters.
   *
   * y = outG * f(cell)
   */
  override fun forward() {

    this.setGates(this.layer.layerContextWindow.getPrevStateLayer()) // must be called before accessing to the activated values of the gates

    val y: DenseNDArray = this.layer.outputArray.values
    val outG: DenseNDArray = this.layer.outputGate.values
    val cellA: DenseNDArray = this.layer.cell.values

    y.assignProd(outG, cellA)
  }

  /**
   * Forward the input to the output combining it with the parameters, saving the contributions.
   *
   * @param layerContributions the structure in which to save the contributions during the calculations
   */
  override fun forward(layerContributions: LayerParameters<*>) {
    TODO("not implemented")
  }

  /**
   * Set gates values
   *
   * inG = sigmoid(wIn (dot) x + bIn + wInRec (dot) yPrev)
   * outG = sigmoid(wOut (dot) x + bOut + wOutRec (dot) yPrev)
   * forG = sigmoid(wFor (dot) x + bFor + wForRec (dot) yPrev)
   * cand = f(wCand (dot) x + bC + wCandRec (dot) yPrev)
   * cell = inG * cand + forG * cellPrev
   */
  private fun setGates(prevStateLayer: LayerStructure<*>?) {

    this.forwardGates()

    if (prevStateLayer != null) {
      this.addGatesRecurrentContribution(prevStateLayer)
    }

    this.activateGates()

    val cell: DenseNDArray = this.layer.cell.values
    val inG: DenseNDArray = this.layer.inputGate.values
    val cand: DenseNDArray = this.layer.candidate.values
    cell.assignProd(inG, cand)

    if (prevStateLayer != null) { // add recurrent contribution to the cell
      val forG: DenseNDArray = this.layer.forgetGate.values
      val cellPrev: DenseNDArray = (prevStateLayer as LSTMLayerStructure).cell.valuesNotActivated
      cell.assignSum(forG.prod(cellPrev))
    }

    this.layer.cell.activate()
  }

  /**
   *
   */
  private fun forwardGates() { this.layer.params as LSTMLayerParameters

    val x: InputNDArrayType = this.layer.inputArray.values

    this.layer.inputGate.forward(this.layer.params.inputGate, x)
    this.layer.outputGate.forward(this.layer.params.outputGate, x)
    this.layer.forgetGate.forward(this.layer.params.forgetGate, x)
    this.layer.candidate.forward(this.layer.params.candidate, x)
  }

  /**
   *
   */
  private fun addGatesRecurrentContribution(prevStateLayer: LayerStructure<*>) {
    this.layer.params as LSTMLayerParameters

    val yPrev: DenseNDArray = prevStateLayer.outputArray.values

    this.layer.inputGate.addRecurrentContribution(this.layer.params.inputGate, yPrev)
    this.layer.outputGate.addRecurrentContribution(this.layer.params.outputGate, yPrev)
    this.layer.forgetGate.addRecurrentContribution(this.layer.params.forgetGate, yPrev)
    this.layer.candidate.addRecurrentContribution(this.layer.params.candidate, yPrev)
  }

  /**
   *
   */
  private fun activateGates() {
    this.layer.inputGate.activate()
    this.layer.outputGate.activate()
    this.layer.forgetGate.activate()
    this.layer.candidate.activate()
  }
}
