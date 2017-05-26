/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package ndarray

import com.kotlinnlp.simplednn.core.functionalities.randomgenerators.RandomGenerator
import com.kotlinnlp.simplednn.simplemath.concatVectorsV
import com.kotlinnlp.simplednn.simplemath.equals
import com.kotlinnlp.simplednn.simplemath.ndarray.DenseNDArray
import com.kotlinnlp.simplednn.simplemath.ndarray.DenseNDArrayFactory
import com.kotlinnlp.simplednn.simplemath.ndarray.Shape
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import kotlin.test.*

/**
 *
 */
class NDArraySpec : Spek({

  describe("an NDArray") {

    context("class factory methods") {

      on("arrayOf()") {

        val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0))

        it("should return an NDArray") {
          assertEquals(true, array is DenseNDArray)
        }

        it("should have the expected number of rows") {
          assertEquals(true, array.rows == 4)
        }

        it("should have the expected number of columns") {
          assertEquals(true, array.columns == 1)
        }

        it("should contain the expected value at index 0") {
          assertEquals(0.1, array[0])
        }

        it("should contain the expected value at index 1") {
          assertEquals(0.2, array[1])
        }

        it("should contain the expected value at index 2") {
          assertEquals(0.3, array[2])
        }

        it("should contain the expected value at index 3") {
          assertEquals(0.0, array[3])
        }
      }

      on("zeros()") {

        val array = DenseNDArrayFactory.zeros(Shape(2, 3))

        it("should return an NDArray") {
          assertEquals(true, array is DenseNDArray)
        }

        it("should have the expected number of rows") {
          assertEquals(true, array.rows == 2)
        }

        it("should have the expected number of columns") {
          assertEquals(true, array.columns == 3)
        }

        it("should be filled with zeros") {
          (0 until array.length).forEach({ assertEquals(0.0, array[it]) })
        }
      }

      on("emptyArray()") {

        val array = DenseNDArrayFactory.emptyArray(Shape(3, 2))

        it("should return an NDArray") {
          assertEquals(true, array is DenseNDArray)
        }

        it("should have the expected number of rows") {
          assertEquals(3, array.rows)
        }

        it("should have the expected number of columns") {
          assertEquals(2, array.columns)
        }
      }

      on("oneHotEncoder()") {

        val array = DenseNDArrayFactory.oneHotEncoder(length = 4, oneAt = 2)

        it("should return an NDArray") {
          assertEquals(true, array is DenseNDArray)
        }

        it("should have the expected length") {
          assertEquals(4, array.length)
        }

        it("should be a column vector") {
          assertEquals(1, array.columns)
        }

        it("should have the expected values") {
          assertEquals(true, DenseNDArrayFactory.arrayOf(doubleArrayOf(0.0, 0.0, 1.0, 0.0)).equals(array))
        }
      }

      on("random()") {

        val array = DenseNDArrayFactory.random(shape = Shape(216, 648), from = 0.5, to = 0.89)

        it("should return an NDArray") {
          assertEquals(true, array is DenseNDArray)
        }

        it("should have the expected number of rows") {
          assertEquals(216, array.rows)
        }

        it("should have the expected number of columns") {
          assertEquals(648, array.columns)
        }

        it("should contain values within the expected range") {
          (0 until array.length).forEach({ i ->
            val value = array[i]
            assertTrue { value >= 0.5 && value < 0.89 }
          })
        }
      }
    }

    context("equality with tolerance") {

      val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.123, 0.234, 0.345, 0.012))

      on("comparison within the tolerance") {

        val arrayToCompare = DenseNDArrayFactory.arrayOf(
          doubleArrayOf(0.123000001, 0.234000001, 0.345000001, 0.012000001))

        it("should result equal with a large tolerance") {
          assertEquals(true, array.equals(arrayToCompare, tolerance=1.0e-03))
        }

        it("should result equal with a strict tolerance") {
          assertEquals(true, array.equals(arrayToCompare, tolerance=1.0e-08))
        }
      }

      on("comparison out of tolerance") {

        val arrayToCompare = DenseNDArrayFactory.arrayOf(
          doubleArrayOf(0.12303, 0.23403, 0.34503, 0.01203))

        it("should result not equal") {
          assertEquals(false, array.equals(arrayToCompare, tolerance=1.0e-05))
        }
      }
    }

    context("initialization through a double array of 4 elements") {

      val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0))

      on("properties") {

        it("should be a vector") {
          assertEquals(true, array.isVector)
        }

        it("should not be a matrix") {
          assertEquals(false, array.isMatrix)
        }

        it("should have the expected length") {
          assertEquals(4, array.length)
        }

        it("should have the expected number of rows") {
          assertEquals(4, array.rows)
        }

        it("should have the expected number of columns") {
          assertEquals(1, array.columns)
        }

        it("should have the expected shape") {
          assertEquals(true, array.shape == Shape(4))
        }
      }

      on("generic methods") {

        it("should be equal to itself") {
          assertEquals(true, array.equals(array))
        }

        it("should be equal to its copy") {
          assertEquals(true, array.equals(array.copy()))
        }
      }

      on("getRange() method") {

        val a = array.getRange(0, 3)
        val b = array.getRange(2, 4)

        it("should return a range of the expected length") {
          assertEquals(3, a.length)
        }

        it("should return the expected range (0, 3)") {
          assertEquals(true, DenseNDArrayFactory.arrayOf(doubleArrayOf(
            0.1, 0.2, 0.3)).equals(a))
        }

        it("should return the expected range (2, 4)") {
          assertEquals(true, DenseNDArrayFactory.arrayOf(doubleArrayOf(
            0.3, 0.0)).equals(b))
        }

        it("should raise an IndexOutOfBoundsException requesting for a range out of bounds") {
          assertFailsWith<IndexOutOfBoundsException> {
            array.getRange(2, 6)
          }
        }
      }

      on("transpose") {

        val transposedArray = array.T

        it("should give a transposed array with the expected shape") {
          assertEquals(true, transposedArray.shape == Shape(1, 4))
        }

        it("should give a transposed array with the expected values") {
          assertEquals(transposedArray[2], 0.3)
        }
      }
    }

    context("isOneHotEncoder() method") {

      val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0))
      val oneHotEncoder = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.0, 0.0, 1.0, 0.0))
      val oneHotEncoderFake = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.0, 0.1, 0.0, 0.0))
      val array2 = DenseNDArrayFactory.arrayOf(arrayOf(
        doubleArrayOf(0.1, 0.2, 0.3, 0.0),
        doubleArrayOf(0.1, 0.2, 0.3, 0.0)
      ))

      it("should return false on a random array") {
        assertFalse { array.isOneHotEncoder }
      }

      it("should return false on a 2-dim array") {
        assertFalse { array2.isOneHotEncoder }
      }

      it("should return false on an array with one element equal to 0.1") {
        assertFalse { oneHotEncoderFake.isOneHotEncoder }
      }

      it("should return true on an array with one element equal to 1.0") {
        assertTrue { oneHotEncoder.isOneHotEncoder }
      }
    }

    context("math methods returning a new NDArray") {

      val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0))
      val a = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.4, 0.3, 0.5, 0.7))
      val n = 0.9

      on("sum() method") {

        it("should give the expected sum of its elements") {
          assertEquals(true, equals(0.6, array.sum(), tolerance = 0.005))
        }
      }

      on("sum(number) method") {

        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(1.0, 1.1, 1.2, 0.9))
        val res = array.sum(n)

        it("should return a new NDArray") {
          assertEquals(false, array === res)
        }

        it("should return the expected values") {
          assertEquals(true, res.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("sum(array) method") {

        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.5, 0.5, 0.8, 0.7))
        val res = array.sum(a)

        it("should return a new NDArray") {
          assertEquals(false, array === res)
        }

        it("should return the expected values") {
          assertEquals(true, res.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("sub(number) method") {

        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(-0.8, -0.7, -0.6, -0.9))
        val res = array.sub(n)

        it("should return a new NDArray") {
          assertEquals(false, array === res)
        }

        it("should return the expected values") {
          assertEquals(true, res.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("reverseSub(number) method") {

        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.8, 0.7, 0.6, 0.9))
        val res = array.reverseSub(n)

        it("should return a new NDArray") {
          assertEquals(false, array === res)
        }

        it("should return the expected values") {
          assertEquals(true, res.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("dot(array) method") {

        val expectedArray = DenseNDArrayFactory.arrayOf(arrayOf(
          doubleArrayOf(0.04, 0.03, 0.05, 0.07),
          doubleArrayOf(0.08, 0.06, 0.1, 0.14),
          doubleArrayOf(0.12, 0.09, 0.15, 0.21),
          doubleArrayOf(0.0, 0.0, 0.0, 0.0)
        ))
        val res = array.dot(a.T)

        it("should return a new NDArray") {
          assertEquals(false, array === res)
        }

        it("should throw an error with not compatible shapes") {
          assertFails({ array.dot(a) })
        }

        it("should assign the expected values") {
          assertEquals(true, res.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("prod(number) method") {

        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.09, 0.18, 0.27, 0.0))
        val res = array.prod(n)

        it("should return a new NDArray") {
          assertEquals(false, array === res)
        }

        it("should return the expected values") {
          assertEquals(true, res.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("prod(array) method") {

        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.04, 0.06, 0.15, 0.0))
        val res = array.prod(a)

        it("should return a new NDArray") {
          assertEquals(false, array === res)
        }

        it("should return the expected values") {
          assertEquals(true, res.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("div(number) method") {

        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1111, 0.2222, 0.3333, 0.0))
        val res = array.div(n)

        it("should return a new NDArray") {
          assertEquals(false, array === res)
        }

        it("should return the expected values") {
          assertEquals(true, res.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("div(array) method") {

        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.25, 0.6667, 0.6, 0.0))
        val res = array.div(a)

        it("should return a new NDArray") {
          assertEquals(false, array === res)
        }

        it("should return the expected values") {
          assertEquals(true, res.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("roundInt(threshold) method") {

        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.0, 1.0, 1.0, 0.0))
        val res = array.roundInt(threshold = 0.2)

        it("should return a new NDArray") {
          assertEquals(false, array === res)
        }

        it("should return the expected values") {
          assertEquals(true, res.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("avg() method") {

        it("should return the expected average") {
          assertEquals(true, equals(0.15, array.avg(), tolerance = 1.0e-08))
        }
      }

      on("sign() method") {

        val signedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(-0.1, 0.0, 0.7, -0.6))
        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(-1.0, 0.0, 1.0, -1.0))
        val res = signedArray.sign()

        it("should return a new NDArray") {
          assertEquals(false, signedArray === res)
        }

        it("should return the expected values") {
          assertEquals(true, res.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("sqrt() method") {

        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.3162, 0.4472, 0.5478, 0.0))
        val res = array.sqrt()

        it("should return a new NDArray") {
          assertEquals(false, array === res)
        }

        it("should return the expected values") {
          assertEquals(true, res.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("pow(number) method") {

        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.2399, 0.3687, 0.4740, 0.0))
        val res = array.pow(0.62)

        it("should return a new NDArray") {
          assertEquals(false, array === res)
        }

        it("should return the expected values") {
          assertEquals(true, res.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("norm2() method") {

        it("should return the expected euclidean norm") {
          assertEquals(true, equals(0.37417, array.norm2(), tolerance = 1.0e-05))
        }
      }

      on("argMaxIndex() method") {

        it("should have the expected argmax index") {
          assertEquals(2, array.argMaxIndex())
        }
      }
    }

    context("math methods in-place") {

      val a = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.4, 0.3, 0.5, 0.7))
      val b = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.7, 0.8, 0.1, 0.4))
      val n = 0.9

      on("assignSum(number) method") {

        val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0))
        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(1.0, 1.1, 1.2, 0.9))
        val res = array.assignSum(n)

        it("should return the same NDArray") {
          assertEquals(true, array === res)
        }

        it("should assign the expected values") {
          assertEquals(true, array.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("assignSum(array, number) method") {

        val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0))
        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(1.3, 1.2, 1.4, 1.6))
        val res = array.assignSum(a, n)

        it("should return the same NDArray") {
          assertEquals(true, array === res)
        }

        it("should assign the expected values") {
          assertEquals(true, array.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("assignSum(array, array) method") {

        val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0))
        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(1.1, 1.1, 0.6, 1.1))
        val res = array.assignSum(a, b)

        it("should return the same NDArray") {
          assertEquals(true, array === res)
        }

        it("should assign the expected values") {
          assertEquals(true, array.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("assignSum(array) method") {

        val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0))
        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.5, 0.5, 0.8, 0.7))
        val res = array.assignSum(a)

        it("should return the same NDArray") {
          assertEquals(true, array === res)
        }

        it("should assign the expected values") {
          assertEquals(true, array.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("assignSub(number) method") {

        val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0))
        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(-0.8, -0.7, -0.6, -0.9))
        val res = array.assignSub(n)

        it("should return the same NDArray") {
          assertEquals(true, array === res)
        }

        it("should assign the expected values") {
          assertEquals(true, array.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("assignSub(array) method") {

        val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0))
        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(-0.3, -0.1, -0.2, -0.7))
        val res = array.assignSub(a)

        it("should return the same NDArray") {
          assertEquals(true, array === res)
        }

        it("should assign the expected values") {
          assertEquals(true, array.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("assignDot(array, array[1-d]) method") {

        val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0))
        val a1 = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.28))
        val expectedArray = DenseNDArrayFactory.arrayOf(arrayOf(
          doubleArrayOf(0.112),
          doubleArrayOf(0.084),
          doubleArrayOf(0.14),
          doubleArrayOf(0.196)
        ))
        val res = array.assignDot(a, a1)

        it("should return the same NDArray") {
          assertEquals(true, array === res)
        }

        it("should throw an error with not compatible shapes") {
          assertFails({ array.assignDot(a, b.T) })
        }

        it("should assign the expected values") {
          assertEquals(true, array.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("assignDot(array, array[2-d]) method") {

        val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0))
        val v = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.3, 0.8))
        val m = DenseNDArrayFactory.arrayOf(arrayOf(
          doubleArrayOf(0.7, 0.5),
          doubleArrayOf(0.3, 0.2),
          doubleArrayOf(0.3, 0.5),
          doubleArrayOf(0.7, 0.5)
        ))
        val expectedArray = DenseNDArrayFactory.arrayOf(arrayOf(
          doubleArrayOf(0.61),
          doubleArrayOf(0.25),
          doubleArrayOf(0.49),
          doubleArrayOf(0.61)
        ))
        val res = array.assignDot(m, v)

        it("should return the same NDArray") {
          assertEquals(true, array === res)
        }

        it("should throw an error with not compatible shapes") {
          val m2 = DenseNDArrayFactory.arrayOf(arrayOf(
            doubleArrayOf(0.7, 0.5),
            doubleArrayOf(0.3, 0.2),
            doubleArrayOf(0.3, 0.5),
            doubleArrayOf(0.7, 0.5)
          ))
          assertFails({ array.assignDot(a.T, m2) })
        }

        it("should assign the expected values") {
          assertEquals(true, array.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("assignProd(number) method") {

        val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0))
        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.09, 0.18, 0.27, 0.0))
        val res = array.assignProd(n)

        it("should return the same NDArray") {
          assertEquals(true, array === res)
        }

        it("should assign the expected values") {
          assertEquals(true, array.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("assignProd(array, number) method") {

        val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0))
        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.36, 0.27, 0.45, 0.63))
        val res = array.assignProd(a, n)

        it("should return the same NDArray") {
          assertEquals(true, array === res)
        }

        it("should assign the expected values") {
          assertEquals(true, array.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("assignProd(array, array) method") {

        val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0))
        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.28, 0.24, 0.05, 0.28))
        val res = array.assignProd(a, b)

        it("should return the same NDArray") {
          assertEquals(true, array === res)
        }

        it("should assign the expected values") {
          assertEquals(true, array.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("assignProd(array) method") {

        val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0))
        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.04, 0.06, 0.15, 0.0))
        val res = array.assignProd(a)

        it("should return the same NDArray") {
          assertEquals(true, array === res)
        }

        it("should assign the expected values") {
          assertEquals(true, array.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("assignDiv(number) method") {

        val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0))
        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1111, 0.2222, 0.3333, 0.0))
        val res = array.assignDiv(n)

        it("should return the same NDArray") {
          assertEquals(true, array === res)
        }

        it("should assign the expected values") {
          assertEquals(true, array.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("assignDiv(array) method") {

        val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0))
        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.25, 0.6667, 0.6, 0.0))
        val res = array.assignDiv(a)

        it("should return the same NDArray") {
          assertEquals(true, array === res)
        }

        it("should assign the expected values") {
          assertEquals(true, array.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("assignPow(number) method") {

        val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0))
        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.2399, 0.3687, 0.4740, 0.0))
        val res = array.assignPow(0.62)

        it("should return the same NDArray") {
          assertEquals(true, array === res)
        }

        it("should assign the expected values") {
          assertEquals(true, array.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("assignRoundInt(threshold) method") {

        val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0))
        val expectedArray = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.0, 1.0, 1.0, 0.0))
        val res = array.assignRoundInt(threshold = 0.2)

        it("should return the same NDArray") {
          assertEquals(true, array === res)
        }

        it("should assign the expected values") {
          assertEquals(true, array.equals(expectedArray, tolerance = 1.0e-04))
        }
      }

      on("randomize(randomGenerator) method") {

        val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0))
        val randomGeneratorMock = mock<RandomGenerator>()
        var i = 0
        @Suppress("UNUSED_CHANGED_VALUE")
        whenever(randomGeneratorMock.next()).then { a[i++] } // assign the same values of [a]

        val res = array.randomize(randomGeneratorMock)

        it("should return the same NDArray") {
          assertEquals(true, array === res)
        }

        it("should return the expected values") {
          assertEquals(true, res.equals(a))
        }
      }
    }

    context("initialization through an array of 2 double arrays of 4 elements") {

      val array = DenseNDArrayFactory.arrayOf(arrayOf(
        doubleArrayOf(0.1, 0.2, 0.3, 0.4),
        doubleArrayOf(0.5, 0.6, 0.7, 0.8)
      ))

      on("properties") {

        it("should not be a vector") {
          assertEquals(false, array.isVector)
        }

        it("should be a matrix") {
          assertEquals(true, array.isMatrix)
        }

        it("should have the expected length") {
          assertEquals(8, array.length)
        }

        it("should have the expected number of rows") {
          assertEquals(2, array.rows)
        }

        it("should have the expected number of columns") {
          assertEquals(4, array.columns)
        }

        it("should have the expected shape") {
          assertEquals(true, array.shape == Shape(2, 4))
        }
      }

      on("generic methods") {

        it("should be equal to itself") {
          assertEquals(true, array.equals(array))
        }

        it("should be equal to its copy") {
          assertEquals(true, array.equals(array.copy()))
        }
      }

      on("getRange() method") {

        it("should fail the vertical vector require") {
          assertFailsWith<Throwable> {
            array.getRange(2, 4)
          }
        }
      }

      on("getRow() method") {

        val row = array.getRow(1)

        it("should return a row vector") {
          assertEquals(1, row.rows)
        }

        it("should return the expected row values") {
          assertEquals(true, row.equals(DenseNDArrayFactory.arrayOf(
                  arrayOf(doubleArrayOf(0.5, 0.6, 0.7, 0.8)))))
        }
      }

      on("getColumn() method") {

        val column = array.getColumn(1)

        it("should return a column vector") {
          assertEquals(1, column.columns)
        }

        it("should return the expected column values") {
          assertEquals(true, column.equals(DenseNDArrayFactory.arrayOf(doubleArrayOf(0.2, 0.6))))
        }
      }

      on("transpose") {

        val transposedArray = array.T

        it("should give a transposed array with the expected shape") {
          assertEquals(true, transposedArray.shape == Shape(4, 2))
        }

        it("should give a transposed array with the expected values") {
          assertEquals(transposedArray[2, 1], 0.7)
        }
      }
    }

    context("initialization through zerosLike()") {

      val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0)).zerosLike()
      val arrayOfZeros = array.zerosLike()

      it("should have the expected length") {
        assertEquals(array.length, arrayOfZeros.length)
      }

      it("should have the expected values") {
        assertEquals(true, DenseNDArrayFactory.arrayOf(doubleArrayOf(0.0, 0.0, 0.0, 0.0)).equals(arrayOfZeros))
      }
    }

    context("converting a NDArray to zeros") {

      val array = DenseNDArrayFactory.arrayOf(arrayOf(
        doubleArrayOf(0.1, 0.2, 0.3, 0.4),
        doubleArrayOf(0.5, 0.6, 0.7, 0.8)
      ))

      on("zeros() method call") {

        array.zeros()

        it("should return an NDArray filled with zeros") {
          (0 until array.length).forEach { i -> assertEquals(0.0, array[i]) }
        }
      }

    }

    context("values assignment") {

      on("assignment through another NDArray") {

        val array = DenseNDArrayFactory.emptyArray(Shape(3, 2))
        val arrayToAssign = DenseNDArrayFactory.arrayOf(arrayOf(
                doubleArrayOf(0.1, 0.2),
                doubleArrayOf(0.3, 0.4),
                doubleArrayOf(0.5, 0.6)
        ))

        array.assignValues(arrayToAssign)

        it("should contain the expected assigned values") {
          assertEquals(true, array.equals(arrayToAssign))
        }
      }

      on("assignment through a number") {

        val array = DenseNDArrayFactory.emptyArray(Shape(3, 2))

        array.assignValues(0.6)

        it("should contain the expected assigned values") {
          (0 until array.length).forEach { i -> assertEquals(0.6, array[i]) }
        }
      }
    }

    context("getters") {

      on("a vertical vector") {
        val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0))

        it("should get the correct item") {
          assertEquals(array[2], 0.3)
        }
      }

      on("a horizontal vector") {
        val array = DenseNDArrayFactory.arrayOf(arrayOf(
          doubleArrayOf(0.1),
          doubleArrayOf(0.2),
          doubleArrayOf(0.3),
          doubleArrayOf(0.0)
        ))

        it("should get the correct item") {
          assertEquals(array[2], 0.3)
        }
      }

      on("a matrix") {
        val array = DenseNDArrayFactory.arrayOf(arrayOf(
          doubleArrayOf(0.1, 0.2, 0.3),
          doubleArrayOf(0.4, 0.5, 0.6)
        ))

        it("should get the correct item") {
          assertEquals(array[1, 2], 0.6)
        }
      }
    }

    context("setters") {

      on("a vertical vector") {
        val array = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3, 0.0))

        array[2] = 0.7

        it("should set the correct item") {
          assertEquals(array[2], 0.7)
        }
      }

      on("a horizontal vector") {
        val array = DenseNDArrayFactory.arrayOf(arrayOf(
          doubleArrayOf(0.1),
          doubleArrayOf(0.2),
          doubleArrayOf(0.3),
          doubleArrayOf(0.0)
        ))

        array[2] = 0.7

        it("should get the correct item") {
          assertEquals(array[2], 0.7)
        }
      }

      on("a matrix") {
        val array = DenseNDArrayFactory.arrayOf(arrayOf(
          doubleArrayOf(0.1, 0.2, 0.3),
          doubleArrayOf(0.4, 0.5, 0.6)
        ))

        array[1, 2] = 0.7

        it("should get the correct item") {
          assertEquals(array[1, 2], 0.7)
        }
      }
    }

    context("single horizontal concatenation") {

      val array1 = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3))
      val array2 = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.4, 0.5, 0.6))
      val concatenatedArray = array1.concatH(array2)

      it("should have the expected shape") {
        assertEquals(Shape(3, 2), concatenatedArray.shape)
      }

      it("should have the expected values") {
        assertEquals(true,
          DenseNDArrayFactory.arrayOf(arrayOf(
            doubleArrayOf(0.1, 0.4),
            doubleArrayOf(0.2, 0.5),
            doubleArrayOf(0.3, 0.6))).equals(concatenatedArray))
      }
    }

    context("single vertical concatenation") {

      val array1 = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3))
      val array2 = DenseNDArrayFactory.arrayOf(doubleArrayOf(0.4, 0.5, 0.6))
      val concatenatedArray = array1.concatV(array2)

      it("should have the expected length") {
        assertEquals(6, concatenatedArray.length)
      }

      it("should have the expected values") {
        assertEquals(true, DenseNDArrayFactory.arrayOf(doubleArrayOf(
          0.1, 0.2, 0.3, 0.4, 0.5, 0.6)).equals(concatenatedArray))
      }
    }

    context("multiple vertical concatenation") {

      val concatenatedArray = concatVectorsV(
        DenseNDArrayFactory.arrayOf(doubleArrayOf(0.1, 0.2, 0.3)),
        DenseNDArrayFactory.arrayOf(doubleArrayOf(0.4, 0.5, 0.6)),
        DenseNDArrayFactory.arrayOf(doubleArrayOf(0.7, 0.8, 0.9))
      )

      it("should have the expected length") {
        assertEquals(9, concatenatedArray.length)
      }

      it("should have the expected values") {
        assertEquals(true, DenseNDArrayFactory.arrayOf(doubleArrayOf(
          0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9)).equals(concatenatedArray))
      }
    }
  }
})
