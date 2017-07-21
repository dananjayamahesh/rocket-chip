// See LICENSE.SiFive for license details.

package freechips.rocketchip.tilelink

import Chisel._
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._

/** Specifies widths of various attachement points in the SoC */
trait TLBusParams {
  val beatBytes: Int
  val blockBytes: Int
  val masterBuffering: BufferParams
  val slaveBuffering: BufferParams

  def beatBits: Int = beatBytes * 8
  def blockBits: Int = blockBytes * 8
  def blockBeats: Int = blockBytes / beatBytes
  def blockOffset: Int = log2Up(blockBytes)
}

abstract class TLBusWrapper(params: TLBusParams)(implicit p: Parameters) extends TLBusParams {
  val beatBytes = params.beatBytes
  val blockBytes = params.blockBytes
  val masterBuffering = params.masterBuffering
  val slaveBuffering = params.slaveBuffering
  require(blockBytes % beatBytes == 0)

  private val xbar = LazyModule(new TLXbar)
  private val master_buffer = LazyModule(new TLBuffer(masterBuffering))
  private val master_splitter = LazyModule(new TLSplitter)  // Allows cycle-free connection to external networks
  private val slave_buffer = LazyModule(new TLBuffer(slaveBuffering))
  private val slave_frag = LazyModule(new TLFragmenter(beatBytes, blockBytes))
  private val slave_ww = LazyModule(new TLWidthWidget(beatBytes))

  master_splitter.node :=* master_buffer.node
  xbar.node :=* master_splitter.node

  slave_buffer.node :*= xbar.node
  slave_frag.node :*= slave_buffer.node
  slave_ww.node :*= slave_buffer.node

  protected def outwardNode: TLOutwardNode = xbar.node
  protected def outwardBufNode: TLOutwardNode = slave_buffer.node
  protected def outwardFragNode: TLOutwardNode = slave_frag.node
  protected def outwardWWNode: TLOutwardNode = slave_ww.node
  protected def inwardNode: TLInwardNode = xbar.node
  protected def inwardBufNode: TLInwardNode = master_buffer.node
  protected def inwardSplitNode: TLInwardNode = master_splitter.node
  protected def outwardSplitNode: TLOutwardNode = master_splitter.node

  def edgesIn = xbar.node.edgesIn

  def bufferFromMasters: TLInwardNode = inwardBufNode

  def bufferToSlaves: TLOutwardNode = outwardBufNode 

  def toAsyncSlaves(sync: Int = 3): TLAsyncOutwardNode = {
    val source = LazyModule(new TLAsyncCrossingSource(sync))
    source.node :*= outwardNode
    source.node
  }

  def toRationalSlaves: TLRationalOutwardNode = {
    val source = LazyModule(new TLRationalCrossingSource())
    source.node :*= outwardNode
    source.node
  }

  def toVariableWidthSlaves: TLOutwardNode = outwardFragNode

  def toAsyncVariableWidthSlaves(sync: Int = 3): TLAsyncOutwardNode = {
    val source = LazyModule(new TLAsyncCrossingSource(sync))
    source.node :*= outwardFragNode
    source.node
  }

  def toRationalVariableWidthSlaves: TLRationalOutwardNode = {
    val source = LazyModule(new TLRationalCrossingSource())
    source.node :*= outwardFragNode
    source.node
  }

  def toFixedWidthSlaves: TLOutwardNode = outwardWWNode

  def toAsyncFixedWidthSlaves(sync: Int = 3): TLAsyncOutwardNode = {
    val source = LazyModule(new TLAsyncCrossingSource(sync))
    source.node := outwardWWNode
    source.node
  }

  def toRationalFixedWidthSlaves: TLRationalOutwardNode = {
    val source = LazyModule(new TLRationalCrossingSource())
    source.node :*= outwardWWNode
    source.node
  }

  def toFixedWidthPorts: TLOutwardNode = outwardWWNode // TODO, do/don't buffer here; knowing we will after the necessary port conversions

}
