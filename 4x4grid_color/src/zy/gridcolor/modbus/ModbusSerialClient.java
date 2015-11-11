package zy.gridcolor.modbus;

import android.hardware.usb.UsbManager;
import net.wimpi.modbus.ModbusCoupler;
import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.ModbusIOException;
import net.wimpi.modbus.ModbusSlaveException;
import net.wimpi.modbus.facade.IModbusLogger;
import net.wimpi.modbus.io.ModbusSerialTransaction;
import net.wimpi.modbus.msg.ReadCoilsRequest;
import net.wimpi.modbus.msg.ReadCoilsResponse;
import net.wimpi.modbus.msg.ReadInputRegistersRequest;
import net.wimpi.modbus.msg.ReadInputRegistersResponse;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersResponse;
import net.wimpi.modbus.msg.WriteCoilRequest;
import net.wimpi.modbus.msg.WriteMultipleRegistersRequest;
import net.wimpi.modbus.net.SerialConnection;
import net.wimpi.modbus.procimg.Register;
import net.wimpi.modbus.procimg.SimpleRegister;
import net.wimpi.modbus.usbserial.SerialParameters;

public class ModbusSerialClient {
	private UsbManager m_UsbManager;
	SerialConnection m_connection;
	
	private IModbusLogger m_logger = null;
	
	public ModbusSerialClient(UsbManager UsbManager) {
		m_UsbManager = UsbManager;
	}
	
	public void setLogger(IModbusLogger logger) {
		m_logger = logger;
		
		if (m_connection != null)
			m_connection.setLogger(logger);
	}

	public void connect(String portname) throws Exception {
		//ModbusCoupler.createModbusCoupler(null);
		ModbusCoupler.getReference().setUnitID(1);
		
		SerialParameters params = new SerialParameters();
		params.setPortName(portname);
		params.setBaudRate(19200);
		params.setDatabits(8);
		params.setParity("None");
		params.setStopbits(1);
		params.setEncoding("ascii");
		params.setEcho(false);
		m_connection = new SerialConnection(m_UsbManager, params);
		m_connection.open();
		m_connection.setLogger(m_logger);
	}

	public void disconnect() {
		if (m_connection != null) {
			m_connection.close();
		}
	}
	
	public boolean isConnected() {
		if (m_connection != null)
			return m_connection.isOpen();
		else
			return false;
	}
	
	public int readHoldingRegister(int ref) throws ModbusIOException, ModbusSlaveException, ModbusException  {
    	ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(ref, 1);
    	req.setUnitID(1);
    	
    	ModbusSerialTransaction trans = new ModbusSerialTransaction(m_connection);
    	trans.setRequest(req);
    	
    	trans.execute();
    	
    	ReadMultipleRegistersResponse res = (ReadMultipleRegistersResponse) trans.getResponse();
    	
    	return res.getRegisterValue(0);
	}
	
	public int[] readHoldingRegisters(int ref, int count) throws ModbusIOException, ModbusSlaveException, ModbusException {
    	ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(ref, count);
    	req.setUnitID(1);
    	
    	ModbusSerialTransaction trans = new ModbusSerialTransaction(m_connection);
    	trans.setRequest(req);
    	
    	trans.execute();
    	
    	ReadMultipleRegistersResponse res = (ReadMultipleRegistersResponse) trans.getResponse();
    	
    	int[] values = new int[count];
    	
    	for (int i = 0; i < count; i++) {
    		values[i] = res.getRegisterValue(i);
    	}
    	
    	return values;
	}
	
	public void writeHoldingRegister(int ref, int value) throws ModbusIOException, ModbusSlaveException, ModbusException {
		ModbusSerialTransaction trans = new ModbusSerialTransaction(m_connection);

		Register[] regs = new Register[1];
		regs[0] = new SimpleRegister(value);

		WriteMultipleRegistersRequest Wreq = new WriteMultipleRegistersRequest(ref, regs);
		Wreq.setUnitID(1);
		
		trans.setRequest(Wreq);

		trans.execute();
	}
	
	public void writeHoldingRegisters(int ref, int[] values) throws ModbusIOException, ModbusSlaveException, ModbusException {
		ModbusSerialTransaction trans = new ModbusSerialTransaction(m_connection);

		Register[] regs = new Register[values.length];
		
		for (int i = 0; i < values.length; i++) {
			regs[i] = new SimpleRegister(values[i]);
		}
		
		WriteMultipleRegistersRequest Wreq = new WriteMultipleRegistersRequest(ref, regs);
		Wreq.setUnitID(1);
		
		trans.setRequest(Wreq);

		trans.execute();
	}

	
	public int readInputRegister(int ref) throws ModbusIOException, ModbusSlaveException, ModbusException {
    	ReadInputRegistersRequest req = new ReadInputRegistersRequest(ref, 1);
    	//req.setUnitID(2);
    	
    	ModbusSerialTransaction trans = new ModbusSerialTransaction(m_connection);
    	trans.setRequest(req);
    	
    	trans.execute();
    	
    	ReadInputRegistersResponse res = (ReadInputRegistersResponse) trans.getResponse();
    	
    	return res.getRegisterValue(0);
	}
	
	public int[] readInputRegisters(int ref, int count) throws ModbusIOException, ModbusSlaveException, ModbusException {
    	ReadInputRegistersRequest req = new ReadInputRegistersRequest(ref, count);
    	req.setUnitID(1);
    	
    	ModbusSerialTransaction trans = new ModbusSerialTransaction(m_connection);
    	trans.setRequest(req);
    	
    	trans.execute();
    	
    	ReadInputRegistersResponse res = (ReadInputRegistersResponse) trans.getResponse();
    	
    	int[] values = new int[count];
    	
    	for (int i = 0; i < count; i++) {
    		values[i] = res.getRegisterValue(i);
    	}
    	
    	return values;
	}
	
	public boolean readBoolRegister(int ref) throws ModbusIOException, ModbusSlaveException, ModbusException {
		ReadCoilsRequest req = new ReadCoilsRequest(ref, 1);

		ModbusSerialTransaction trans = new ModbusSerialTransaction(m_connection);
    	trans.setRequest(req);
    	
    	trans.execute();
    	
    	ReadCoilsResponse res = (ReadCoilsResponse) trans.getResponse();
    	
    	return res.getCoilStatus(0);
	}
	
	public void writeBoolRegister(int ref, boolean value) throws ModbusIOException, ModbusSlaveException, ModbusException {
		WriteCoilRequest req = new WriteCoilRequest(ref, value);

		ModbusSerialTransaction trans = new ModbusSerialTransaction(m_connection);
    	trans.setRequest(req);
    	
    	trans.execute();
	}

}
