// $Id: //dvt/vtech/dev/main/ovm/src/tlm/ovm_ports.svh#8 $
//----------------------------------------------------------------------
//   Copyright 2007-2008 Mentor Graphics Corporation
//   Copyright 2007-2008 Cadence Design Systems, Inc.
//   All Rights Reserved Worldwide
//
//   Licensed under the Apache License, Version 2.0 (the
//   "License"); you may not use this file except in
//   compliance with the License.  You may obtain a copy of
//   the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in
//   writing, software distributed under the License is
//   distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
//   CONDITIONS OF ANY KIND, either express or implied.  See
//   the License for the specific language governing
//   permissions and limitations under the License.
//----------------------------------------------------------------------

class ovm_blocking_put_port #(type T=int)
  extends ovm_port_base #(tlm_if_base #(T,T));
  `OVM_PORT_COMMON(`TLM_BLOCKING_PUT_MASK,"ovm_blocking_put_port")
  `BLOCKING_PUT_IMP (this.m_if, T, t)
endclass 

class ovm_nonblocking_put_port #(type T=int)
  extends ovm_port_base #(tlm_if_base #(T,T));
  `OVM_PORT_COMMON(`TLM_NONBLOCKING_PUT_MASK,"ovm_nonblocking_put_port")
  `NONBLOCKING_PUT_IMP (this.m_if, T, t)
endclass

class ovm_put_port #(type T=int)
  extends ovm_port_base #(tlm_if_base #(T,T));
  `OVM_PORT_COMMON(`TLM_PUT_MASK,"ovm_put_port")
  `PUT_IMP (this.m_if, T, t)
endclass

class ovm_blocking_get_port #(type T=int)
  extends ovm_port_base #(tlm_if_base #(T,T));
  `OVM_PORT_COMMON(`TLM_BLOCKING_GET_MASK,"ovm_blocking_get_port")
  `BLOCKING_GET_IMP (this.m_if, T, t)
endclass 

class ovm_nonblocking_get_port #(type T=int)
  extends ovm_port_base #(tlm_if_base #(T,T));
  `OVM_PORT_COMMON(`TLM_NONBLOCKING_GET_MASK,"ovm_nonblocking_get_port")
  `NONBLOCKING_GET_IMP (this.m_if, T, t)
endclass

class ovm_get_port #(type T=int)
  extends ovm_port_base #(tlm_if_base #(T,T));
  `OVM_PORT_COMMON(`TLM_GET_MASK,"ovm_get_port")
  `GET_IMP (this.m_if, T, t)
endclass 

class ovm_blocking_peek_port #(type T=int)
  extends ovm_port_base #(tlm_if_base #(T,T));
  `OVM_PORT_COMMON(`TLM_BLOCKING_PEEK_MASK,"ovm_blocking_peek_port")
  `BLOCKING_PEEK_IMP (this.m_if, T, t)
endclass 

class ovm_nonblocking_peek_port #(type T=int)
  extends ovm_port_base #(tlm_if_base #(T,T));
  `OVM_PORT_COMMON(`TLM_NONBLOCKING_PEEK_MASK,"ovm_nonblocking_peek_port")
  `NONBLOCKING_PEEK_IMP (this.m_if, T, t)
endclass

class ovm_peek_port #(type T=int)
  extends ovm_port_base #(tlm_if_base #(T,T));
  `OVM_PORT_COMMON(`TLM_PEEK_MASK,"ovm_peek_port")
  `PEEK_IMP (this.m_if, T, t)
endclass 

class ovm_blocking_get_peek_port #(type T=int)
  extends ovm_port_base #(tlm_if_base #(T,T));
  `OVM_PORT_COMMON(`TLM_BLOCKING_GET_PEEK_MASK,"ovm_blocking_get_peek_port")
  `BLOCKING_GET_PEEK_IMP (this.m_if, T, t)
endclass 

class ovm_nonblocking_get_peek_port #(type T=int)
  extends ovm_port_base #(tlm_if_base #(T,T));
  `OVM_PORT_COMMON(`TLM_NONBLOCKING_GET_PEEK_MASK,"ovm_nonblocking_get_peek_port")
  `NONBLOCKING_GET_PEEK_IMP (this.m_if, T, t)
endclass

class ovm_get_peek_port #(type T=int)
  extends ovm_port_base #(tlm_if_base #(T,T));
  `OVM_PORT_COMMON(`TLM_GET_PEEK_MASK,"ovm_get_peek_port")
  `GET_PEEK_IMP (this.m_if, T, t)
endclass 

class ovm_blocking_master_port #(type REQ=int, type RSP=int)
  extends ovm_port_base #(tlm_if_base #(REQ, RSP));
  `OVM_PORT_COMMON(`TLM_BLOCKING_MASTER_MASK,"ovm_blocking_master_port")
  `BLOCKING_PUT_IMP (this.m_if, REQ, t)
  `BLOCKING_GET_PEEK_IMP (this.m_if, RSP, t)
endclass 

class ovm_nonblocking_master_port #(type REQ=int, type RSP=int)
  extends ovm_port_base #(tlm_if_base #(REQ, RSP));
  `OVM_PORT_COMMON(`TLM_NONBLOCKING_MASTER_MASK,"ovm_nonblocking_master_port")
  `NONBLOCKING_PUT_IMP (this.m_if, REQ, t)
  `NONBLOCKING_GET_PEEK_IMP (this.m_if, RSP, t)
endclass 

class ovm_master_port #(type REQ=int, type RSP=int)
  extends ovm_port_base #(tlm_if_base #(REQ, RSP));
  `OVM_PORT_COMMON(`TLM_MASTER_MASK,"ovm_master_port")
  `PUT_IMP (this.m_if, REQ, t)
  `GET_PEEK_IMP (this.m_if, RSP, t)
endclass

class ovm_blocking_slave_port #(type REQ=int, type RSP=int)
  extends ovm_port_base #(tlm_if_base #(RSP, REQ));
  `OVM_PORT_COMMON(`TLM_BLOCKING_SLAVE_MASK,"ovm_blocking_slave_port")
  `BLOCKING_PUT_IMP (this.m_if, RSP, t)
  `BLOCKING_GET_PEEK_IMP (this.m_if, REQ, t)
endclass 

class ovm_nonblocking_slave_port #(type REQ=int, type RSP=int)
  extends ovm_port_base #(tlm_if_base #(RSP, REQ));
  `OVM_PORT_COMMON(`TLM_NONBLOCKING_SLAVE_MASK,"ovm_nonblocking_slave_port")
  `NONBLOCKING_PUT_IMP (this.m_if, RSP, t)
  `NONBLOCKING_GET_PEEK_IMP (this.m_if, REQ, t)
endclass 

class ovm_slave_port #(type REQ=int, type RSP=int)
  extends ovm_port_base #(tlm_if_base #(RSP, REQ));
  `OVM_PORT_COMMON(`TLM_SLAVE_MASK,"ovm_slave_port")
  `PUT_IMP (this.m_if, RSP, t)
  `GET_PEEK_IMP (this.m_if, REQ, t)
endclass

class ovm_blocking_transport_port #(type REQ=int, type RSP=int)
  extends ovm_port_base #(tlm_if_base #(REQ, RSP));
  `OVM_PORT_COMMON(`TLM_BLOCKING_TRANSPORT_MASK,"ovm_blocking_transport_port")
  `BLOCKING_TRANSPORT_IMP (this.m_if, REQ, RSP, req, rsp)
endclass

class ovm_nonblocking_transport_port #(type REQ=int, type RSP=int)
  extends ovm_port_base #(tlm_if_base #(REQ, RSP));
  `OVM_PORT_COMMON(`TLM_NONBLOCKING_TRANSPORT_MASK,"ovm_nonblocking_transport_port")
  `NONBLOCKING_TRANSPORT_IMP (this.m_if, REQ, RSP, req, rsp)
endclass

class ovm_transport_port #(type REQ=int, type RSP=int)
  extends ovm_port_base #(tlm_if_base #(REQ, RSP));
  `OVM_PORT_COMMON(`TLM_TRANSPORT_MASK,"ovm_transport_port")
  `TRANSPORT_IMP (this.m_if, REQ, RSP, req, rsp)
endclass

class ovm_analysis_port # (type T = int)
  extends ovm_port_base # (tlm_if_base #(T,T));

  function new (string name, ovm_component parent);
    super.new (name, parent, OVM_PORT, 0, OVM_UNBOUNDED_CONNECTIONS);
    m_if_mask = `TLM_ANALYSIS_MASK;  
  endfunction

  virtual function string get_type_name();
    return "ovm_analysis_port";
  endfunction

  // analysis port differs from other ports in that it broadcasts
  // to all connected interfaces. Ports only send to the interface
  // at the index specified in a call to set_if (0 by default).
  function void write (input T t);
    tlm_if_base # (T, T) tif;
    for (int i = 0; i < this.size(); i++) begin
      tif = this.get_if (i);
      assert (tif != null);
      tif.write (t);
    end 
  endfunction

endclass
