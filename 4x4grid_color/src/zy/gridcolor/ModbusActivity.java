package zy.gridcolor;

import zy.grid.color.opencv.R;
import zy.gridcolor.modbus.ModbusSerialClient;
//import net.wimpi.modbus.facade.IModbusLogger;
//import net.wimpi.modbus.facade.ModbusSerialMaster;
//import net.wimpi.modbus.procimg.InputRegister;
//import net.wimpi.modbus.procimg.SimpleRegister;
import net.wimpi.modbus.usbserial.SerialParameters;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
///import java.util.Locale;

public class ModbusActivity extends Activity /*implements IModbusLogger*/ {
    
    private MenuItem             menuBackToMain = null;
    
	private static final int INTERVAL = 1000;
	
	UsbManager m_UsbManager;
	
	ModbusSerialClient m_modbus;

	public static ModbusActivity mModbusActivity;
	
	private Handler m_sensorHandler = new Handler();
	private Runnable m_sensor = new Runnable() {

		@Override
		public void run() {
			try {
				readValues();
			}
			finally {
				m_sensorHandler.postDelayed(this, INTERVAL);
			}
		}
		
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_modbus);
		
        m_UsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        
		SerialParameters params = new SerialParameters();
		params.setPortName("COM1");
		params.setBaudRate(19200);
		params.setDatabits(8);
		params.setParity("None");
		params.setStopbits(1);
		params.setEncoding("ascii");
		params.setEcho(false);

		m_modbus = new ModbusSerialClient(m_UsbManager);
		//m_modbus.setLogger(this);
		
		mModbusActivity = this;
		
		setupConnectButton();
		setupDisconnectButton();
		setupWriteButton();
		setupReadButton();
		setupClearLogButton();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    
	    menuBackToMain = menu.add("Go Back to Main");
	    
		return true;
	}
	
	private void returnToMain() {
        Intent intent = new Intent(this, OpenCVMainActivity.class);
        startActivity(intent);
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
	    
	    if(item == menuBackToMain){
	        returnToMain();
	    }
	    
	    return true;
	}

	@Override
	public void onResume() {
		super.onResume();
		
		if (connect()) {
			m_sensorHandler.postDelayed(m_sensor, INTERVAL);
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		disconnect();
	}
	
	public void onDestroy() {
        super.onDestroy();
        disconnect();
    }
	
//	public void log(final String msg) {
//	    runOnUiThread(new Runnable() {
//	        public void run() {
//	            //Toast.makeText(MainActivity.mMainActivity, msg, Toast.LENGTH_SHORT).show();
//	    		TextView tv = (TextView)findViewById(R.id.text_error);
//	    		String eol = System.getProperty("line.separator");  
//	    		tv.append(eol);
//	    		tv.append(msg);
//	        }
//	    });
//	}

	private void setupConnectButton() {
		Button b = (Button)findViewById(R.id.button_connection);
		
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				connect();
			}
			
		});
	}

	private void setupDisconnectButton() {
		Button b = (Button)findViewById(R.id.button_disconnection);
		
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				disconnect();
			}
			
		});
	}

	private void setupWriteButton() {
		Button b = (Button)findViewById(R.id.button_write);
		
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				write();
			}
			
		});
	}

	private void setupReadButton() {
		Button b = (Button)findViewById(R.id.button_read);
		
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				read();
			}
			
		});
	}

	private void setupClearLogButton() {
		Button b = (Button)findViewById(R.id.button_clear_log);
		
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				TextView tv = (TextView)findViewById(R.id.text_error);
				tv.setText("");
			}
			
		});
	}
	
	private boolean connect() {
		try {
			m_modbus.connect("COM1");
			
			TextView tv = (TextView)findViewById(R.id.label_connectedness);
			tv.setText("connected");
			
			//get initial read
			read();
			
			clearError();
			
			return true;
		}
		catch (Throwable e) {
			showError(e);
			return false;
		}
	}
	
	private void disconnect() {
		try {
			m_modbus.disconnect();
			
			TextView tv = (TextView)findViewById(R.id.label_connectedness);
			tv.setText("disconnected");
			
			clearError();
		}
		catch (Throwable e) {
			showError(e);
		}
	}
	
	private void write() {
		EditText ref = (EditText)findViewById(R.id.edit_ref);
		EditText value = (EditText)findViewById(R.id.edit_value);
		
		try {
			m_modbus.writeHoldingRegister(Integer.parseInt(ref.getText().toString()),
					Integer.parseInt(value.getText().toString()));
			clearError();
		}
		catch (Throwable e) {
			showError(e);
		}
	}
	
	private void read() {
		EditText ref = (EditText)findViewById(R.id.edit_ref);
		EditText value = (EditText)findViewById(R.id.edit_value);
		
		try {
			int v = m_modbus.readHoldingRegister(Integer.parseInt(ref.getText().toString()));
			
			value.setText(Integer.toString(v));
			clearError();
		}
		catch (Throwable e) {
			showError(e);
		}
	}
	
	private void readValues() {
		if (!m_modbus.isConnected())
			return;
		
		try {
			int[] values = m_modbus.readHoldingRegisters(0, 3);
			if (values.length != 3)
				return;
			
			int target = values[0];
			int go = values[1];
			int status = values[2];
			
			TextView text = (TextView)findViewById(R.id.text_target);
			text.setText(Integer.toString(target));

			text = (TextView)findViewById(R.id.text_go);
			text.setText(Integer.toString(go));

			text = (TextView)findViewById(R.id.text_status);
			text.setText(Integer.toString(status));
		}
		catch (Throwable e) {
			showError(e);
		}
	}
	
	private void clearError() {
		TextView tv = (TextView)findViewById(R.id.text_error);
		tv.setText("");
	}
	
	private void showError(Throwable e) {
		e = getInnerError(e);

		TextView tv = (TextView)findViewById(R.id.text_error);
		
		String msg = e.getMessage();
		
		if (msg != null)
			tv.append(msg);
		else
			tv.append(e.toString());

		String eol = System.getProperty("line.separator");  
		tv.append(eol);
		
		tv.append(toString(e.getStackTrace()));
	}

	private Throwable getInnerError(Throwable e) {
		while (e.getCause() != null) {
			e = e.getCause();
		}
		
		return e;
	}
	
	public static String toString(StackTraceElement[] stackTraceElements) {
	    if (stackTraceElements == null)
	        return "";
	    StringBuilder stringBuilder = new StringBuilder();
	    for (StackTraceElement element : stackTraceElements)
	        stringBuilder.append(element.toString()).append("\n");
	    return stringBuilder.toString();
	}
}
