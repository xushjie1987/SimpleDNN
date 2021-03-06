/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.simplednn.core.neuralprocessor.recurrent

import com.kotlinnlp.simplednn.core.arrays.DistributionArray
import com.kotlinnlp.simplednn.core.layers.LayerParameters
import com.kotlinnlp.simplednn.core.layers.LayerStructure
import com.kotlinnlp.simplednn.core.layers.LayerType
import com.kotlinnlp.simplednn.core.layers.models.recurrent.GatedRecurrentLayerStructure
import com.kotlinnlp.simplednn.core.layers.models.recurrent.RecurrentLayerStructure
import com.kotlinnlp.simplednn.core.layers.models.merge.MergeLayer
import com.kotlinnlp.simplednn.core.neuralnetwork.NetworkParameters
import com.kotlinnlp.simplednn.core.neuralnetwork.NeuralNetwork
import com.kotlinnlp.simplednn.core.neuralnetwork.structure.recurrent.RecurrentNetworkStructure
import com.kotlinnlp.simplednn.core.neuralnetwork.structure.recurrent.StructureContextWindow
import com.kotlinnlp.simplednn.core.neuralprocessor.NeuralProcessor
import com.kotlinnlp.simplednn.core.optimizer.ParamsErrorsAccumulator
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArrayFactory
import com.kotlinnlp.simplednn.simplemath.ndarray.NDArray
import com.kotlinnlp.simplednn.simplemath.ndarray.Shape

/**
 * The [RecurrentNeuralProcessor] acts on the [neuralNetwork] performing predictions
 * and training based on sequences of recurrent Examples.
 *
 * @property neuralNetwork a [NeuralNetwork]
 * @property id an identification number useful to track a specific processor
 */
class RecurrentNeuralProcessor<InputNDArrayType : NDArray<InputNDArrayType>>(
  neuralNetwork: NeuralNetwork,
  id: Int = 0
) : StructureContextWindow<InputNDArrayType>,
  NeuralProcessor(neuralNetwork = neuralNetwork, id = id) {

  /**
   * Sequence of states.
   */
  private val sequence = NNSequence<InputNDArrayType>(neuralNetwork)

  /**
   * Set each time a single forward or a single backward are called
   */
  private var curStateIndex: Int = 0

  /**
   * An index which indicates the last state (-1 if the sequence is empty).
   */
  private var lastStateIndex: Int = -1

  /**
   * The total amount of states processed in the current sequence.
   */
  private val statesSize: Int
    get() = this.lastStateIndex + 1

  /**
   * The helper which calculates the importance scores of all the previous states of a given one, in a RAN neural
   * network.
   */
  private val ranImportanceHelper: RANImportanceHelper by lazy { RANImportanceHelper() }

  /**
   * The errors of the network model parameters calculated during a single backward
   */
  private val backwardParamsErrors: NetworkParameters by lazy {
    this.neuralNetwork.parametersFactory(forceDense = false)
  }

  /**
   *
   */
  private val paramsErrorsAccumulator by lazy { ParamsErrorsAccumulator<NetworkParameters>() }

  /**
   *
   */
  private val zeroErrors: DenseNDArray by lazy {
    DenseNDArrayFactory.zeros(shape = Shape(this.neuralNetwork.layersConfiguration.last().size))
  }

  /**
   *
   */
  override fun getPrevStateStructure(): RecurrentNetworkStructure<InputNDArrayType>? =
    if (this.curStateIndex in 1 .. this.lastStateIndex)
      this.sequence.getStateStructure(this.curStateIndex - 1)
    else
      null

  /**
   *
   */
  override fun getNextStateStructure(): RecurrentNetworkStructure<InputNDArrayType>? =
    if (this.curStateIndex < this.lastStateIndex) // it works also for the init hidden structure
      this.sequence.getStateStructure(this.curStateIndex + 1)
    else
      null

  /**
   *
   */
  fun getOutput(copy: Boolean = true): DenseNDArray =
    if (copy)
      this.sequence.getStateStructure(this.lastStateIndex).outputLayer.outputArray.values.copy()
    else
      this.sequence.getStateStructure(this.lastStateIndex).outputLayer.outputArray.values

  /**
   * @param copy a Boolean indicating whether the returned errors must be a copy or a reference
   *
   * @return the accumulated errors of the network parameters
   */
  override fun getParamsErrors(copy: Boolean): NetworkParameters =
    this.paramsErrorsAccumulator.getParamsErrors(copy = copy)

  /**
   * Get the input errors of all the elements of the last forwarded sequence, in the same order.
   * This method should be called after a backward.
   *
   * @param copy a Boolean indicating whether the returned errors must be a copy or a reference
   *
   * @return an array containing the errors of the input sequence
   */
  fun getInputSequenceErrors(copy: Boolean = true): List<DenseNDArray> = List(
    size = this.statesSize,
    init = { i -> this.getInputErrors(elementIndex = i, copy = copy) }
  )

  /**
   * Get the inputs errors of all the elements of the last forwarded sequence, in the same order, in case of the input
   * layer is a Merge layer.
   * This method should be called after a backward.
   *
   * @param copy a Boolean indicating whether the returned errors must be a copy or a reference
   *
   * @return an array containing the errors of the input sequence
   */
  fun getInputsSequenceErrors(copy: Boolean = true): List<List<DenseNDArray>> = List(
    size = this.statesSize,
    init = { i -> this.getInputsErrors(elementIndex = i, copy = copy) }
  )

  /**
   * Get the input errors of an element of the last forwarded sequence.
   * This method must be used when the input layer is not a Merge layer and it should be called after a backward.
   *
   * @param elementIndex the index of an element of the input sequence
   * @param copy a Boolean indicating whether the returned errors must be a copy or a reference
   *
   * @return the input errors of the network structure at the given index of the sequence
   */
  fun getInputErrors(elementIndex: Int, copy: Boolean = true): DenseNDArray {

    require(elementIndex in 0 .. this.lastStateIndex) {
      "element index (%d) must be within the length of the sequence in the range [0, %d]"
        .format(elementIndex, this.lastStateIndex)
    }

    val structure: RecurrentNetworkStructure<InputNDArrayType> = this.sequence.getStateStructure(elementIndex)

    require(structure.inputLayer !is MergeLayer<InputNDArrayType>)

    return structure.inputLayer.inputArray.let { if (copy) it.errors.copy() else it.errors }
  }

  /**
   * Get the inputs errors of an element of the last forwarded sequence, in case of the input layer is a Merge layer.
   * This method must be used when the input layer is a Merge layer and it should be called after a backward.
   *
   * @param elementIndex the index of an element of the input sequence
   * @param copy a Boolean indicating whether the returned errors must be a copy or a reference
   *
   * @return the list of inputs errors of the network structure at the given index of the sequence
   */
  fun getInputsErrors(elementIndex: Int, copy: Boolean = true): List<DenseNDArray> {

    require(elementIndex in 0 .. this.lastStateIndex) {
      "element index (%d) must be within the length of the sequence in the range [0, %d]"
        .format(elementIndex, this.lastStateIndex)
    }

    val structure: RecurrentNetworkStructure<InputNDArrayType> = this.sequence.getStateStructure(elementIndex)

    require(structure.inputLayer is MergeLayer<InputNDArrayType>)

    return (structure.inputLayer as MergeLayer<InputNDArrayType>).inputArrays.map {
      if (copy) it.errors.copy() else it.errors
    }
  }

  /**
   * Get the errors of the initial hidden arrays.
   * This method should be used only if initial hidden arrays has been passed in the last [forward] call.
   *
   * @return the errors of the initial hidden arrays (null if no init hidden is used for a certain layer)
   */
  fun getInitHiddenErrors(): List<DenseNDArray?> = this.sequence.getStateStructure(0).getInitHiddenErrors()

  /**
   * @param copy a Boolean indicating whether the returned arrays must be a copy or a reference
   *
   * @return the output sequence
   */
  fun getOutputSequence(copy: Boolean = true): List<DenseNDArray> = List(
    size = this.statesSize,
    init = { i ->
      this.sequence.getStateStructure(i).outputLayer.outputArray.values.let { if (copy) it.copy() else it }
    }
  )

  /**
   * Forward a sequence.
   *
   * Set the [initHiddenArrays] to use them as previous hidden in the first forward. Set some of them to null to don't
   * use them for certain layers.
   *
   * @param sequenceFeatures the features to forward for each item of the sequence
   * @param initHiddenArrays the list of initial hidden arrays (one per layer, null by default)
   * @param saveContributions whether to save the contributions of each input to its output (needed to calculate
   *                          the relevance)
   * @param useDropout whether to apply the dropout
   *
   * @return the last output of the network after the whole sequence is been forwarded
   */
  fun forward(sequenceFeatures: List<InputNDArrayType>,
              initHiddenArrays: List<DenseNDArray?>? = null,
              saveContributions: Boolean = false,
              useDropout: Boolean = false): DenseNDArray {

    sequenceFeatures.forEachIndexed { i, features ->
      this.forward(
        features = features,
        firstState = (i == 0),
        initHiddenArrays = initHiddenArrays,
        saveContributions = saveContributions,
        useDropout = useDropout)
    }

    return this.getOutput()
  }

  /**
   * Forward features.
   *
   * Set the [initHiddenArrays] to use them as previous hidden in the first forward. Set some of them to null to don't
   * use them for certain layers.
   * [initHiddenArrays] will be ignored if [firstState] is false.
   *
   * @param features the features to forward from the input to the output
   * @param firstState whether the current one is the first state
   * @param initHiddenArrays the list of initial hidden arrays (one per layer, null by default)
   * @param saveContributions whether to save the contributions of each input to its output (needed to calculate
   *                          the relevance)
   * @param useDropout whether to apply the dropout
   */
  fun forward(features: InputNDArrayType,
              firstState: Boolean,
              initHiddenArrays: List<DenseNDArray?>? = null,
              saveContributions: Boolean = false,
              useDropout: Boolean = false): DenseNDArray {

    if (firstState) this.reset()

    this.addNewState(saveContributions = saveContributions)

    this.curStateIndex = this.lastStateIndex // crucial to provide the right context

    this.forwardCurrentState(
      features = features,
      initHiddenArrays = initHiddenArrays,
      saveContributions = saveContributions,
      useDropout = useDropout)

    return this.getOutput()
  }

  /**
   * Forward a sequence when the input layer is a Merge layer.
   *
   * Set the [initHiddenArrays] to use them as previous hidden in the first forward. Set some of them to null to don't
   * use them for certain layers.
   *
   * @param sequenceFeaturesLists the list of features to forward for each item of the sequence
   * @param initHiddenArrays the list of initial hidden arrays (one per layer, null by default)
   * @param saveContributions whether to save the contributions of each input to its output (needed to calculate
   *                          the relevance)
   * @param useDropout whether to apply the dropout
   *
   * @return the last output of the network after the whole sequence is been forwarded
   */
  fun forward(sequenceFeaturesLists: ArrayList<List<InputNDArrayType>>,
              initHiddenArrays: List<DenseNDArray?>? = null,
              saveContributions: Boolean = false,
              useDropout: Boolean = false): DenseNDArray {

    sequenceFeaturesLists.forEachIndexed { i, featuresList ->
      this.forward(
        featuresList = featuresList,
        firstState = (i == 0),
        initHiddenArrays = initHiddenArrays,
        saveContributions = saveContributions,
        useDropout = useDropout)
    }

    return this.getOutput()
  }

  /**
   * Forward features when the input layer is a Merge layer.
   *
   * Set the [initHiddenArrays] to use them as previous hidden in the first forward. Set some of them to null to don't
   * use them for certain layers.
   * [initHiddenArrays] will be ignored if [firstState] is false.
   *
   * @param featuresList the list of features to forward from the input to the output
   * @param firstState whether the current one is the first state
   * @param initHiddenArrays the list of initial hidden arrays (one per layer, null by default)
   * @param saveContributions whether to save the contributions of each input to its output (needed to calculate
   *                          the relevance)
   * @param useDropout whether to apply the dropout
   */
  fun forward(featuresList: List<InputNDArrayType>,
              firstState: Boolean,
              initHiddenArrays: List<DenseNDArray?>? = null,
              saveContributions: Boolean = false,
              useDropout: Boolean = false): DenseNDArray {

    if (firstState) {
      this.reset()
    }

    this.addNewState(saveContributions = saveContributions)

    this.curStateIndex = this.lastStateIndex // crucial to provide the right context

    this.forwardCurrentState(
      featuresList = featuresList,
      initHiddenArrays = initHiddenArrays,
      saveContributions = saveContributions,
      useDropout = useDropout)

    return this.getOutput()
  }

  /**
   * Calculate the relevance of the input of a state respect to the output of another (or the same) state.
   *
   * @param stateFrom the index of the state from whose input to calculate the relevance
   * @param stateTo the index of the state whose output will be used as reference to calculate the relevance
   * @param relevantOutcomesDistribution the distribution which indicates which outcomes are relevant, used
   *                                     as reference to calculate the relevance of the input
   * @param copy whether to return a copy of the relevance or not
   *
   * @return the relevance of the input of the state [stateFrom] respect of the output of the state [stateTo]
   */
  fun calculateRelevance(stateFrom: Int,
                         stateTo: Int,
                         relevantOutcomesDistribution: DistributionArray,
                         copy: Boolean = true): NDArray<*> {

    require(stateFrom <= stateTo) { "stateFrom (%d) must be <= stateTo (%d)".format(stateFrom, stateTo) }
    require(stateFrom in 0 .. this.lastStateIndex) {
      "stateFrom (%d) index exceeded sequence size (%d)".format(stateFrom, this.statesSize)
    }

    this.sequence.getStateStructure(stateTo).layers.last().setOutputRelevance(relevantOutcomesDistribution)

    for (stateIndex in (stateFrom .. stateTo).reversed()) {
      this.curStateIndex = stateIndex // crucial to provide the right context
      this.propagateRelevanceOnCurrentState(isFirstState = stateIndex == stateFrom, isLastState = stateIndex == stateTo)
    }

    return this.getInputRelevance(stateIndex = stateFrom, copy = copy)
  }

  /**
   * Get the importance scores of the previous states respect of a given state.
   * The scores values are in the range [0.0, 1.0].
   *
   * This method should be called only after a [forward] call.
   * It is required that the network structures contain only a RAN layer.
   *
   * @param stateIndex the index of a state
   *
   * @return the array containing the importance scores of the previous states
   */
  fun getRANImportanceScores(stateIndex: Int): DenseNDArray {

    require(stateIndex > 0) { "Cannot get the importance score of the first state." }
    require(this.neuralNetwork.layersConfiguration.count { it.connectionType == LayerType.Connection.RAN } == 1) {
      "It is required that only one layer must be a RAN layer to get the RAN importance score."
    }

    return this.ranImportanceHelper.getImportanceScores(
      states = (0 .. stateIndex).map { this.sequence.getStateStructure(it) })
  }

  /**
   * Backward errors.
   *
   * @param outputErrors the errors of the output
   * @param propagateToInput whether to propagate the errors to the input (default = false)
   * @param mePropK a list of k factors (one per layer) of the 'meProp' algorithm to propagate from the k (in
   *                percentage) output nodes with the top errors of each layer (the list and each element can be null)
   */
  fun backward(outputErrors: DenseNDArray, propagateToInput: Boolean = false, mePropK: List<Double?>? = null) {

    val outputErrorsSequence = List(
      size = this.statesSize,
      init = { i -> if (i == this.lastStateIndex) outputErrors else this.zeroErrors }
    )

    this.backward(outputErrorsSequence = outputErrorsSequence, propagateToInput = propagateToInput, mePropK = mePropK)
  }

  /**
   * Backward errors of a sequence.
   *
   * @param outputErrorsSequence output errors for each item of the sequence
   * @param propagateToInput whether to propagate the errors to the input (default = false)
   * @param mePropK a list of k factors (one per layer) of the 'meProp' algorithm to propagate from the k (in
   *                percentage) output nodes with the top errors of each layer (the list and each element can be null)
   */
  fun backward(outputErrorsSequence: List<DenseNDArray>,
               propagateToInput: Boolean = false,
               mePropK: List<Double?>? = null) {

    require(outputErrorsSequence.size == (this.statesSize)) {
      "Number of errors (%d) does not reflect the length of the sequence (%d)"
        .format(outputErrorsSequence.size, this.statesSize)
    }

    for (i in (0 .. this.lastStateIndex).reversed()) {
      this.backwardStep(outputErrors = outputErrorsSequence[i], propagateToInput = propagateToInput, mePropK = mePropK)
    }
  }

  /**
   * One single step of backward, respect to the last forwarded sequence, starting from the last element.
   * Each time this function is called, the focus state index decrease of 1.
   *
   * @param outputErrors output errors of the current item of the sequence
   * @param propagateToInput whether to propagate the errors to the input (default = false)
   * @param mePropK a list of k factors (one per layer) of the 'meProp' algorithm to propagate from the k (in
   *                percentage) output nodes with the top errors of each layer (the list and each element can be null)
   */
  fun backwardStep(outputErrors: DenseNDArray,
                   propagateToInput: Boolean = false,
                   mePropK: List<Double?>? = null) {

    require(this.curStateIndex <= this.lastStateIndex) {
      "The current state (%d) cannot be greater then the last state index (%d)."
        .format(this.curStateIndex, this.lastStateIndex)
    }

    this.sequence.getStateStructure(this.curStateIndex).backward(
      outputErrors = outputErrors,
      paramsErrors = this.backwardParamsErrors,
      propagateToInput = propagateToInput,
      mePropK = mePropK)

    this.paramsErrorsAccumulator.accumulate(this.backwardParamsErrors)

    if (this.curStateIndex == 0) this.paramsErrorsAccumulator.averageErrors()

    this.curStateIndex--
  }

  /**
   * Add a new state.
   *
   * @param saveContributions whether to save the contributions of each input to its output (needed to calculate the
   *        relevance)
   */
  private fun addNewState(saveContributions: Boolean = false) {

    // TODO: save always contributions?? (structures are created only when it's needed)

    if (this.lastStateIndex == this.sequence.lastIndex) {

      val structure = RecurrentNetworkStructure(
        layersConfiguration = this.neuralNetwork.layersConfiguration,
        params = this.neuralNetwork.model,
        structureContextWindow = this)

      this.sequence.add(structure = structure, saveContributions = saveContributions)
    }

    this.lastStateIndex++
  }

  /**
   * Forward the current state.
   *
   * @param features the features to forward from the input to the output
   * @param initHiddenArrays the list of initial hidden arrays (one per layer, can be null)
   * @param saveContributions whether to save the contributions of each input to its output (needed to calculate the
   *                          relevance)
   * @param useDropout whether to apply the dropout
   */
  private fun forwardCurrentState(
    features: InputNDArrayType,
    initHiddenArrays: List<DenseNDArray?>?,
    saveContributions: Boolean,
    useDropout: Boolean = false) {

    val structure: RecurrentNetworkStructure<InputNDArrayType> = this.sequence.getStateStructure(this.lastStateIndex)

    structure.setInitHidden(arrays = if (this.curStateIndex == 0) initHiddenArrays else null)

    if (saveContributions)
      structure.forward(
        features = features,
        networkContributions = this.sequence.getStateContributions(this.lastStateIndex),
        useDropout = useDropout)
    else
      structure.forward(
        features = features,
        useDropout = useDropout)
  }

  /**
   * Forward the current state when the input layer is a Merge layer.
   *
   * @param featuresList the list of features to forward from the input to the output
   * @param initHiddenArrays the list of initial hidden arrays (one per layer, can be null)
   * @param saveContributions whether to save the contributions of each input to its output (needed to calculate the
   *                          relevance)
   * @param useDropout whether to apply the dropout
   */
  private fun forwardCurrentState(
    featuresList: List<InputNDArrayType>,
    initHiddenArrays: List<DenseNDArray?>?,
    saveContributions: Boolean,
    useDropout: Boolean = false) {

    val structure: RecurrentNetworkStructure<InputNDArrayType> = this.sequence.getStateStructure(this.lastStateIndex)

    structure.setInitHidden(arrays = if (this.curStateIndex == 0) initHiddenArrays else null)

    if (saveContributions) {
      structure.forward(
        featuresList = featuresList,
        networkContributions = this.sequence.getStateContributions(this.lastStateIndex),
        useDropout = useDropout)

    } else {
      structure.forward(
        featuresList = featuresList,
        useDropout = useDropout)
    }
  }

  /**
   * Propagate the relevance backward through the layers of the current state.
   *
   * @param isFirstState a Boolean indicating if the current state is the first
   * @param isLastState a Boolean indicating if the current state is the last
   */
  private fun propagateRelevanceOnCurrentState(isFirstState: Boolean, isLastState: Boolean) {

    val structure: RecurrentNetworkStructure<InputNDArrayType> = this.sequence.getStateStructure(this.curStateIndex)
    var isPropagating: Boolean = isLastState

    for ((layerIndex, layer) in structure.layers.withIndex().reversed()) {

      structure.curLayerIndex = layerIndex // crucial to provide the right context

      val isCurLayerRecurrent = layer is RecurrentLayerStructure
      val isPrevLayerRecurrent = layerIndex > 0 && structure.layers[layerIndex - 1] is RecurrentLayerStructure

      isPropagating = isPropagating || isCurLayerRecurrent

      if (isPropagating) {
        this.propagateLayerRelevance(
          layer = layer,
          layerIndex = layerIndex,
          propagateToPrevState = !isFirstState && isCurLayerRecurrent,
          propagateToInput = layerIndex > 0 || isFirstState,
          replaceInputRelevance = isLastState || !isPrevLayerRecurrent
        )
      }
    }
  }

  /**
   * Propagate the relevance backward through the current layer.
   *
   * @param layer the current layer
   * @param layerIndex the current layer index
   * @param propagateToPrevState whether to propagate to the previous state
   * @param propagateToInput whether to propagate to the input
   * @param replaceInputRelevance a Boolean if the relevance of the input must be replaced or added
   */
  private fun propagateLayerRelevance(layer: LayerStructure<*>,
                                      layerIndex: Int,
                                      propagateToPrevState: Boolean,
                                      propagateToInput: Boolean,
                                      replaceInputRelevance: Boolean) {

    require(propagateToInput || propagateToPrevState)

    val contributions: LayerParameters<*>
      = this.sequence.getStateContributions(this.curStateIndex).paramsPerLayer[layerIndex]

    if (layer is GatedRecurrentLayerStructure) {
      layer.propagateRelevanceToGates(layerContributions = contributions)
    }

    if (propagateToInput) {

      if (replaceInputRelevance)
        layer.setInputRelevance(layerContributions = contributions)
      else
        layer.addInputRelevance(layerContributions = contributions)
    }

    if (propagateToPrevState) {
      (layer as RecurrentLayerStructure).setRecurrentRelevance(layerContributions = contributions)
    }
  }

  /**
   * Get the relevance of the input of a state of the sequence.
   * (If the input is Dense it is Dense, if the input is Sparse or SparseBinary it is Sparse).
   *
   * @param stateIndex the index of the state from which to extract the input relevance
   * @param copy whether to return a copy of the relevance or not
   *
   * @return the relevance of the input as [NDArray]
   */
  private fun getInputRelevance(stateIndex: Int, copy: Boolean = true): NDArray<*> =
    this.sequence.getStateStructure(stateIndex).inputLayer.inputArray.relevance.let { if (copy) it.copy() else it }

  /**
   * Reset the sequence.
   */
  private fun reset() {
    this.lastStateIndex = -1
    this.paramsErrorsAccumulator.reset()
  }
}
