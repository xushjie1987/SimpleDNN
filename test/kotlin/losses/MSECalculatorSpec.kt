/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

import com.kotlinnlp.simplednn.core.functionalities.losses.MSECalculator
import com.kotlinnlp.simplednn.simplemath.NDArray
import com.kotlinnlp.simplednn.simplemath.equals
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 *
 */
class MSECalculatorSpec : Spek({

  describe("a MSECalculator") {

    val lossCalculator = MSECalculator()

    context("single output") {

      val outputValues = NDArray.arrayOf(doubleArrayOf(0.0, 0.1, 0.2, 0.3))
      val goldValues = NDArray.arrayOf(doubleArrayOf(0.3, 0.2, 0.1, 0.0))

      on("calculateErrors") {
        val errors = lossCalculator.calculateErrors(outputValues, goldValues)

        it("should calculate the expected errors") {
          assertTrue(NDArray.arrayOf(doubleArrayOf(-0.3, -0.1, 0.1, 0.3)).equals(errors))
        }
      }

      on("calculateLoss") {
        val loss = lossCalculator.calculateLoss(outputValues, goldValues)

        it("should calculate the expected loss") {
          assertTrue(NDArray.arrayOf(doubleArrayOf(0.045, 0.005, 0.005, 0.045)).equals(loss))
        }
      }
    }

    context("output sequence") {

      val outputValuesSequence = arrayOf(
        NDArray.arrayOf(doubleArrayOf(0.0, 0.1, 0.2, 0.3)),
        NDArray.arrayOf(doubleArrayOf(0.5, 0.1, 0.4, 0.7))
      )
      val goldValuesSequence = arrayOf(
        NDArray.arrayOf(doubleArrayOf(0.3, 0.2, 0.1, 0.0)),
        NDArray.arrayOf(doubleArrayOf(0.6, 0.9, 0.1, 0.0))
      )

      on("calculateErrors of a sequence of length 2") {

        val sequenceErrors = lossCalculator.calculateErrors(outputValuesSequence, goldValuesSequence)

        it("should return an array of length 2") {
          assertEquals(2, sequenceErrors.size)
        }

        it("should calculate the expected errors of the first element") {
          assertTrue(NDArray.arrayOf(doubleArrayOf(-0.3, -0.1, 0.1, 0.3)).equals(sequenceErrors[0]))
        }

        it("should calculate the expected errors of the second element") {
          assertTrue(NDArray.arrayOf(doubleArrayOf(-0.1, -0.8, 0.3, 0.7)).equals(sequenceErrors[1]))
        }
      }

      on("calculateMeanLoss of a sequence of length 2") {
        val meanLoss = lossCalculator.calculateMeanLoss(outputValuesSequence, goldValuesSequence)

        it("should calculate the expected mean loss") {
          assertTrue(equals(0.089375, meanLoss, tolerance = 1.0e-08))
        }
      }
    }
  }
})