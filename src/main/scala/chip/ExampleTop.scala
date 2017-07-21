// See LICENSE.SiFive for license details.

package freechips.rocketchip.chip

import Chisel._

import freechips.rocketchip.config.Parameters
import freechips.rocketchip.util.HeterogeneousBag

/** Example system with periphery devices (w/o coreplex) */
abstract class ExampleSystem(implicit p: Parameters) extends BaseSystem
    with HasPeripheryAsyncExtInterrupts
    with HasPeripheryMasterAXI4MemPort
    with HasPeripheryMasterAXI4MMIOPort
    with HasPeripherySlaveAXI4Port
    with HasPeripheryErrorSlave
    with HasPeripheryZeroSlave {
  override lazy val module = new ExampleSystemModule(this)
}

class ExampleSystemModule[+L <: ExampleSystem](_outer: L) extends BaseSystemModule(_outer)
    with HasPeripheryExtInterruptsModuleImp
    with HasPeripheryMasterAXI4MemPortModuleImp
    with HasPeripheryMasterAXI4MMIOPortModuleImp
    with HasPeripherySlaveAXI4PortModuleImp

/** Example Top with periphery and a Rocket coreplex */
class ExampleRocketTop(implicit p: Parameters) extends ExampleSystem
    with HasPeripheryBootROM
    with HasPeripheryDebug
    with HasPeripheryRTCCounter
    with HasRocketPlexMaster {
  override lazy val module = new ExampleRocketTopModule(this)
}

class Thing extends Bundle {
  val bla = Bool(INPUT)
  val fa = Bool(OUTPUT)
}
class MyBundleWithEmptyStuff(c: Int) extends Bundle {
  val barNone = HeterogeneousBag(Seq.fill(c) {new Thing()})
  val bazNone = HeterogeneousBag(Seq.fill(c) {new Thing()})
}

class MySimilarBundleWithEmptyStuff(c: Int) extends Bundle {
  val bazNone = HeterogeneousBag(Seq.fill(c) {new Thing()})
  val barNone = HeterogeneousBag(Seq.fill(c) {new Thing()})
}

class InnerModuleWithEmptyThings extends Module {
  val io = new MyBundleWithEmptyStuff(0)

  io.barNone.foreach ( x => printf("Hooray BarNone %d!", x.bla))
  io.bazNone.foreach ( x => printf("Hooray BazNone %d!", x.bla))

  io.bazNone.foreach {x => x.fa := x.bla}
  io.barNone.foreach {x => x.fa := x.bla}
}

class OuterModuleWithEmptyThings extends Module {
  val io = new MySimilarBundleWithEmptyStuff(0)

  val inner = Module(new InnerModuleWithEmptyThings())

  io.barNone <> inner.io.barNone
  io.bazNone <> inner.io.bazNone
 
}

class ExampleRocketTopModule[+L <: ExampleRocketTop](_outer: L) extends ExampleSystemModule(_outer)
    with HasPeripheryBootROMModuleImp
    with HasPeripheryDebugModuleImp
    with HasPeripheryRTCCounterModuleImp
    with HasRocketPlexMasterModuleImp {

  val test = Module(new OuterModuleWithEmptyThings)

  test.io.barNone.foreach ( x => printf("Hooray BarNone %d!", x.fa))
  test.io.bazNone.foreach ( x => printf("Hooray BazNone %d!", x.fa))
    
}
