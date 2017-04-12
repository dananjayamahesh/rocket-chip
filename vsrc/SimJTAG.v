// See LICENSE.SiFive for license details.

import "DPI-C" function int jtag_tick
(
 output bit jtag_TCK,
 output bit jtag_TMS,
 output bit jtag_TDI,
 output bit jtag_TRSTn,
 
 input bit  jtag_TDO
);

module SimJTAG #(
                 parameter TICK_DELAY = 50
                 )(
                   
                   input         enable,
                   input         init_done,

                   output        jtag_TCK,
                   output        jtag_TMS,
                   output        jtag_TDI,
                   output        jtag_TRSTn,
 
                   input         jtag_TDO_data,
                   input         jtag_TDO_driven,
                          
                   output [31:0] exit
);

   bit          r_reset;

   wire [31:0]  random_bits = $random;
   
   wire         #0.1 __jtag_TDO = jtag_TDO_driven ? 
                jtag_TDO_data : random_bits[0];
      
   bit          __jtag_TCK;
   bit          __jtag_TMS;
   bit          __jtag_TDI;
   bit          __jtag_TRSTn;
   int          __exit;
   
   assign #0.1 jtag_TCK   = __jtag_TCK;
   assign #0.1 jtag_TMS   = __jtag_TMS;
   assign #0.1 jtag_TDI   = __jtag_TDI;
   assign #0.1 jtag_TRSTn = __jtag_TRSTn;
   
   assign #0.1 exit = __exit;

   initial begin
      __exit = 0;

      forever begin
         #TICK_DELAY  
         if (enable && init_done) begin
            __exit = jtag_tick(
                               __jtag_TCK,
                               __jtag_TMS,
                               __jtag_TDI,
                               __jtag_TRSTn,
                               __jtag_TDO);
         end
      end // always @ (posedge clk)
   end
endmodule