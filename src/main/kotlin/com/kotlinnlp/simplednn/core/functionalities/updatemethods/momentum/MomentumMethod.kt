/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.simplednn.core.functionalities.updatemethods.momentum

import com.kotlinnlp.simplednn.core.arrays.UpdatableDenseArray
import com.kotlinnlp.simplednn.core.functionalities.updatemethods.UpdateMethod
import com.kotlinnlp.simplednn.core.functionalities.decaymethods.DecayMethod
import com.kotlinnlp.simplednn.core.functionalities.decaymethods.HyperbolicDecay
import com.kotlinnlp.simplednn.core.functionalities.regularization.WeightsRegularization
import com.kotlinnlp.simplednn.core.functionalities.updatemethods.UpdaterSupportStructure
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray
import com.kotlinnlp.simplednn.simplemath.ndarray.sparse.SparseNDArray
import com.kotlinnlp.simplednn.utils.scheduling.EpochScheduling

/**
 * The Momentum method.
 *
 * @param learningRate Double >= 0. Learning rate
 * @param momentum  Double >= 0. Parameter updates momentum
 */
open class MomentumMethod(
  val learningRate: Double = 0.01,
  val momentum: Double = 0.9,
  val decayMethod: DecayMethod? = null,
  regularization: WeightsRegularization? = null
) : EpochScheduling,
  UpdateMethod<MomentumStructure>(regularization) {

  /**
   * @param array the array from which to extract the support structure
   *
   * @return the [UpdaterSupportStructure] extracted from the given [array]
   */
  override fun getSupportStructure(array: UpdatableDenseArray): MomentumStructure = array.getOrSetSupportStructure()

  /**
   *
   */
  var alpha: Double = this.learningRate
    private set

  /**
   *
   */
  private var epochCount: Int = 0

  /**
   * Method to call every new epoch
   */
  override fun newEpoch() {

    if (this.decayMethod != null) {
      this.alpha = this.decayMethod.update(
        learningRate = if (this.decayMethod is HyperbolicDecay) this.learningRate else this.alpha,
        timeStep = ++this.epochCount
      )
    }
  }

  /**
   * Optimize sparse errors.
   * Update velocity with adapted learning rate.
   *
   * @param errors the [SparseNDArray] errors to optimize
   * @param supportStructure the support structure of the [UpdateMethod]
   *
   * @return optimized sparse errors
   */
  override fun optimizeSparseErrors(errors: SparseNDArray, supportStructure: MomentumStructure): SparseNDArray {

    val v = supportStructure.v
    val mask = errors.mask

    v.assignValues(errors.prod(this.alpha).assignSum(v.prod(this.momentum, mask = mask)), mask = mask)

    return v.maskBy(mask)
  }

  /**
   * Optimize dense errors.
   * Update velocity with adapted learning rate.
   *
   * @param errors the [DenseNDArray] errors to optimize
   * @param supportStructure the support structure of the [UpdateMethod]
   *
   * @return optimized dense errors
   */
  override fun optimizeDenseErrors(errors: DenseNDArray, supportStructure: MomentumStructure): DenseNDArray {

    val v = supportStructure.v

    v.assignSum(errors.prod(this.alpha), v.prod(this.momentum))

    return v
  }
}
