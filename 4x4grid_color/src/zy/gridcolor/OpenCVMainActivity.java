package zy.gridcolor;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
//import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

//import org.opencv.imgcodecs.*;
//import org.opencv.videoio.*;
import zy.grid.color.opencv.R;
import zy.gridcolor.modbus.ModbusSerialClient;

import org.opencv.imgproc.Imgproc;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
//import android.R.layout;
import android.app.Activity;
//import android.content.Context;
//import android.content.pm.PackageManager;
//import android.hardware.Camera;
//import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
//import android.widget.EditText;
//import android.widget.TextView;
import android.widget.Toast;
import net.wimpi.modbus.usbserial.SerialParameters;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.usb.UsbManager;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class OpenCVMainActivity extends Activity implements CvCameraViewListener2 {
    
    private static final String  TAG              = "gridcolor::Activity";
    
    private static final int[][] POS_MASK = {
                                                {0x0001, 0x0002, 0x0004, 0x0008},
                                                {0x0010, 0x0020, 0x0040, 0x0080},
                                                {0x0100, 0x0200, 0x0400, 0x0800},
                                                {0x1000, 0x2000, 0x4000, 0x8000}
                                            };
    private String[][] POS_Color = {
            {"LED not ON", "LED not ON", "LED not ON", "LED not ON"},
            {"LED not ON", "LED not ON", "LED not ON", "LED not ON"},
            {"LED not ON", "LED not ON", "LED not ON", "LED not ON"},
            {"LED not ON", "LED not ON", "LED not ON", "LED not ON"}
        };

    //private MenuItem			 menuWhiteAndBlack = null;
    private MenuItem             menuRun = null;
    private MenuItem             menuStop = null;
    private boolean              RunFlag = false;
    private int                  Target_Data = 0;
    
    private MenuItem             menuModbusTest = null;
    private boolean				 GrayMode = false;
    //private int					 widthCamara = 800;
    private int					 hightCamara = 480;
    private TextView text_debug_red;
    private String debug_red;
    private TextView text_debug_green;
    private String debug_green;
    UsbManager m_UsbManager;
    ModbusSerialClient m_modbus;
    //run handler
    private static final int RUN_INTERVAL = 1000;
    private Handler m_runHandler = new Handler();
    private Runnable m_run = new Runnable() {

        @Override
        public void run() {
            try {
                if(RunFlag) {
                    write_target(Target_Data);
                    //do a check, write all the time or? 
                }
            }
            finally {
                m_runHandler.postDelayed(this, RUN_INTERVAL);
            }
        }
        
    };
    
    public void write_target(int target) {
        if (!m_modbus.isConnected())
            return;
        
        try {
            m_modbus.writeHoldingRegister(0, target);
        }
        catch (Throwable e) {
            //
        }
    }
    
    //Camera
    private CameraBridgeViewBase camara;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    camara.enableView();  
                    //MeasureScreenSize();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public OpenCVMainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        
        super.onCreate(savedInstanceState);
        
        //
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
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        //Attempt Full screen, OpenCV do not want in galaxy nexus, yes in galaxy 4 :(
        getWindow().setFlags(
        		WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //Screen ON Permanent

        //Permanent maximum brightness
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        //layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        layoutParams.screenBrightness = -1;
        Log.i(TAG, "opencv_main_activity");
        setContentView(R.layout.opencv_main_activity);
        
        Log.i(TAG, "camara java");
        camara = (CameraBridgeViewBase)findViewById(R.id.camara_java);

    	camara.setVisibility(SurfaceView.VISIBLE);
    	camara.setCvCameraViewListener(this);
    	//Measure Screen Size
    	//MeasureScreenSize();
        

        final float start=-4;
        final float end=4;
        float start_pos=0;
        int start_position=0;
        
		//start = -4; // you need to give starting value of SeekBar
		//end = 4; // you need to give end value of SeekBar
		start_pos = -4; // you need to give starting position value of SeekBar

		start_position = (int) (((start_pos - start) / (end - start)) * 8);
		//discrete = start_pos;
		SeekBar seek = (SeekBar) findViewById(R.id.seekBar1);
		seek.setMax(8);
		seek.incrementProgressBy(1);
		seek.setProgress(start_position);

		seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
	    	int discrete=0;
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				Toast.makeText(getBaseContext(), "Exposure = " + discrete, Toast.LENGTH_SHORT).show();
				camara.setExposure(discrete);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				// TODO Auto-generated method stub
				// To convert it as discrete value
				float temp=progress;
				float dis=end-start;
				discrete=(int) (start+((temp/8)*dis));
			}
		});
		
		text_debug_red = (TextView) findViewById(R.id.textview_red);
		text_debug_green = (TextView) findViewById(R.id.textview_green);
    }
    
    
    
    @Override
    public void onPause()
    {
        super.onPause();
        if (camara != null)
            camara.disableView();
        
        disconnect();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        
        if (connect()) {
            m_runHandler.postDelayed(m_run, RUN_INTERVAL);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (camara != null)
            camara.disableView();
        
        disconnect();
    }
    
    private boolean connect() {
        try {
            m_modbus.connect("COM1");
            return true;
        }
        catch (Throwable e) {
            //showError(e);
            Toast toast = Toast.makeText(this, "Modbus Connect Fail!" , Toast.LENGTH_LONG);
            toast.show();
            return false;
        }
    }
    
    private void disconnect() {
        try {
            m_modbus.disconnect();
        }
        catch (Throwable e) {
            //showError(e);
        }
    }
    
    //We create the menu and options
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        
        menuRun = menu.add("Run");
        menuStop = menu.add("Stop");
        
        menuModbusTest = menu.add("Modbus Test");
        
        //menuWhiteAndBlack = menu.add("White and Black");
        
        SubMenu subMenu = menu.addSubMenu(4, 4, 4, "Select a Resolution");
        subMenu.add(1, 10, 1, "Auto Measure");
        subMenu.add(1, 11, 2, "Full HD resolution (1920x1080)");
        subMenu.add(1, 12, 3, "High resolution (1280x720)");
        subMenu.add(1, 13, 4, "Medium Resolution (960x720)");
        subMenu.add(1, 14, 5, "Low Resolution (800x480)");
        
        return true;
    }
    
    private void testModbus() {
        Intent intent = new Intent(this, ModbusActivity.class);
        startActivity(intent);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        //Button puts black and white - Grey
//        if(item == menuWhiteAndBlack){
//        	if(GrayMode){
//        	    GrayMode = false;
//        		Toast toast = Toast.makeText(this, "'Mode Gray' deactivated.\n'Mode Normal' enable." , Toast.LENGTH_LONG);
//        		toast.show();
//        	}else{
//        	    GrayMode = true;
//        		Toast toast = Toast.makeText(this, "'Mode Normal' deactivated.\n'Mode Gray' enable." , Toast.LENGTH_LONG);
//        		toast.show();
//        	}
//        }
        //End Mode Gray
        
        if(item == menuRun)
        {
            if (!m_modbus.isConnected())
            {
                Toast toast = Toast.makeText(this, "Can not run, modbus not connected!" , Toast.LENGTH_LONG);
                toast.show();
            }
            else
            {
                //start the camera detection and Modbus data sending process
                Target_Data = 0;
                RunFlag = true;
            }
        }
        if(item == menuStop)
        {
            //stop the camera detection and Modbus data sending process
            Target_Data = 0;
            RunFlag = false;
        }
        
        //Button to start the Modbus activity
        if(item == menuModbusTest){
            testModbus();
        }
        
        //Sub Menu to resize the HUD
        switch(item.getItemId()){
            case 10: //Id menu to verify that you have pressed
                MeasureScreenSize();
                Toast toast = Toast.makeText(this, "Resolution Auto Measure" , Toast.LENGTH_LONG);
                toast.show();
                break;
            case 11:
                //widthCamara = 1920;
                hightCamara = 1080;
                toast = Toast.makeText(this, "Resolution of HUD maximum FULL HD" , Toast.LENGTH_LONG);
                toast.show();
                break;
            case 12:
                //widthCamara = 1280;
            	hightCamara = 720;
            	toast = Toast.makeText(this, "Resolution of HUD HD" , Toast.LENGTH_LONG);
        		toast.show();
                break;
            case 13:
                //widthCamara = 960;
            	hightCamara = 720;
            	toast = Toast.makeText(this, "Resolution of HUD medium" , Toast.LENGTH_LONG);
        		toast.show();
                break;
            case 14:
                //widthCamara = 800;
            	hightCamara = 480;
            	toast = Toast.makeText(this, "Resolution of HUD minimum" , Toast.LENGTH_LONG);
        		toast.show();
                break;
        }
        return true;
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
        
    }
    
	public Mat onCameraFrame(CvCameraViewFrame frame) {

		if (GrayMode) {
			// Mode white and black
			return frame.gray();
		} else {
			// Mat, then work in the frame pixels
			Mat mat = frame.rgba();
			Mat saturated;

			double saturation = 10;
			double scale_sat = 3;

			// what it does here is dst = (uchar) ((double)src*scale+saturation); 
			//mat.convertTo(mat, mat.type(), scale_sat, saturation);
			// may or may not need blur
			Imgproc.GaussianBlur(mat, mat, new Size(11, 11), 0);
			hightCamara = mat.height();
			int target_temp = 0;
			int target_temp_green = 0;
			Scalar box_color = new Scalar(0,0,0,0);
			
			int[] box_red = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
			int[] box_green = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
			//Amount of pixels to verify before sending
			int detect_amt = 20;
			//factor should be in scale with javacameraview -> frameSize.width*factor
			int new_factor = 2; 
			//Set width, height for area of detection
			//space is the space between each area
			//col = x , row = y ; This is the starting position upper left of 1st area
			//current max is 960x540
			//Ensure (row1_col+row1space*i+row1width)<960
			//Ensure (row1_row+row1height)<960x540
			int row1width=125/new_factor, row1height=65/new_factor, row1space=315/new_factor;
			int row1_row=120/new_factor, row1_col=320/new_factor;
			for (int i = 0; i < 4; i++) {
				Imgproc.rectangle(mat, new Point(row1_col+row1space*i, row1_row),new Point(row1_col+row1space*i+row1width, row1_row+row1height),box_color, 2);
				search1: {
					for (int mat_row = row1_row; mat_row < row1_row+row1height; mat_row++) {
						for (int mat_col = row1_col+row1space*i; mat_col < row1_col+row1space*i+row1width; mat_col++) {
							double[] CellColorTest = mat.get(mat_row, mat_col);
							String ColorNameTest = getColorName(CellColorTest[0], CellColorTest[1], CellColorTest[2]);
							if (ColorNameTest == "Primary Green")
								box_red[i+1]++;
							if (ColorNameTest == "Primary Red")
								box_green[i+1]++;
							if (box_red[i+1] > detect_amt) {
								Imgproc.putText(mat, "Primary Red", new Point(mat_col, mat_row), 3, 0.5,
										new Scalar(255, 0, 0, 255), 1);
								target_temp |= POS_MASK[0][i];
								break search1;
							}
							if (box_green[i+1] > detect_amt) {
								Imgproc.putText(mat, "Primary Green", new Point(mat_col, mat_row), 3, 0.5,
										new Scalar(255, 0, 0, 255), 1);
								target_temp_green |= POS_MASK[0][i];
								break search1;
							}
						}
					}
				}
			}
			int row2width=(125+10)/new_factor, row2height=(65+10)/new_factor, row2space=(315+10)/new_factor;
			int row2_row=315/new_factor, row2_col=310/new_factor;
			for (int i = 0; i < 4; i++) {
				Imgproc.rectangle(mat, new Point(row2_col+row2space*i, row2_row),new Point(row2_col+row2space*i+row2width, row2_row+row2height),box_color, 2);
				search2: {
					for (int mat_row = row2_row; mat_row < row2_row+row2height; mat_row++) {
						for (int mat_col = row2_col+row2space*i; mat_col < row2_col+row2space*i+row2width; mat_col++) {
							double[] CellColorTest = mat.get(mat_row, mat_col);
							String ColorNameTest = getColorName(CellColorTest[0], CellColorTest[1], CellColorTest[2]);
							if (ColorNameTest == "Primary Green")
								box_red[i+5]++;
							if (ColorNameTest == "Primary Red")
								box_green[i+5]++;
							if (box_red[i+5] > detect_amt) {
								Imgproc.putText(mat, "Primary Red", new Point(mat_col, mat_row), 3, 0.5,
										new Scalar(255, 0, 0, 255), 1);
								target_temp |= POS_MASK[1][i];
								break search2;
							}
							if (box_green[i+5] > detect_amt) {
								Imgproc.putText(mat, "Primary Green", new Point(mat_col, mat_row), 3, 0.5,
										new Scalar(255, 0, 0, 255), 1);
								target_temp_green |= POS_MASK[1][i];
								break search2;
							}
						}
					}
				}
			}
			int row3width=(125+20)/new_factor, row3height=(65+20)/new_factor, row3space=(315+20)/new_factor;
			int row3_row=510/new_factor, row3_col=300/new_factor;
			for (int i = 0; i < 4; i++) {
				Imgproc.rectangle(mat, new Point(row3_col+row3space*i, row3_row),new Point(row3_col+row3space*i+row3width, row3_row+row3height),box_color, 2);
				search3: {
					for (int mat_row = row3_row; mat_row < row3_row+row3height; mat_row++) {
						for (int mat_col = row3_col+row3space*i; mat_col < row3_col+row3space*i+row3width; mat_col++) {
							double[] CellColorTest = mat.get(mat_row, mat_col);
							String ColorNameTest = getColorName(CellColorTest[0], CellColorTest[1], CellColorTest[2]);
							if (ColorNameTest == "Primary Green")
								box_red[i+9]++;
							if (ColorNameTest == "Primary Red")
								box_green[i+9]++;
							if (box_red[i+9] > detect_amt) {
								Imgproc.putText(mat, "Primary Red", new Point(mat_col, mat_row), 3, 0.5,
										new Scalar(255, 0, 0, 255), 1);
								target_temp |= POS_MASK[2][i];
								break search3;
							}
							if (box_green[i+9] > detect_amt) {
								Imgproc.putText(mat, "Primary Green", new Point(mat_col, mat_row), 3, 0.5,
										new Scalar(255, 0, 0, 255), 1);
								target_temp_green |= POS_MASK[2][i];
								break search3;
							}
						}
					}
				}
			}
			int row4width=(125+20)/new_factor, row4height=(65+20)/new_factor, row4space=(315+20)/new_factor;
			int row4_row=705/new_factor, row4_col=300/new_factor;
			for (int i = 0; i < 4; i++) {
				Imgproc.rectangle(mat, new Point(row4_col+row4space*i, row4_row),new Point(row4_col+row4space*i+row4width, row4_row+row4height),box_color, 2);
				search4: {
					for (int mat_row = row4_row; mat_row < row4_row+row4height; mat_row++) {
						for (int mat_col = row4_col+row4space*i; mat_col < row4_col+row4space*i+row4width; mat_col++) {
							double[] CellColorTest = mat.get(mat_row, mat_col);
							String ColorNameTest = getColorName(CellColorTest[0], CellColorTest[1], CellColorTest[2]);
							if (ColorNameTest == "Primary Green")
								box_red[i+13]++;
							if (ColorNameTest == "Primary Red")
								box_green[i+13]++;
							if (box_red[i+13] > detect_amt) {
								Imgproc.putText(mat, "Primary Red", new Point(mat_col, mat_row), 3, 0.5,
										new Scalar(255, 0, 0, 255), 1);
								target_temp |= POS_MASK[3][i];
								break search4;
							}
							if (box_green[i+13] > detect_amt) {
								Imgproc.putText(mat, "Primary Green", new Point(mat_col, mat_row), 3, 0.5,
										new Scalar(255, 0, 0, 255), 1);
								target_temp_green |= POS_MASK[3][i];
								break search4;
							}
						}
					}
				}
			}
			//Log.d("POS", Integer.toString(target_temp,2));
			StringBuilder red_stringbuilder = new StringBuilder();
			red_stringbuilder.append("Red: ");
			StringBuilder green_stringbuilder = new StringBuilder();
			green_stringbuilder.append("Green: ");
			for(int x=0;x<16;x++){
				int value = (int)(Math.pow(2,x));
				if((target_temp & value)==value){
					red_stringbuilder.append(x+1);
					red_stringbuilder.append(",");
				}
				if((target_temp_green & value)==value){
					green_stringbuilder.append(x+1);
					green_stringbuilder.append(",");
				}
			}
			debug_red = red_stringbuilder.toString();
			debug_green = green_stringbuilder.toString();
			
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					text_debug_red.setText(debug_red);
					text_debug_green.setText(debug_green);
				}
			});
			

			if (RunFlag) {

				int round=1; //change this to change rounds
				if(target_temp==0 &&target_temp_green>0 &&round==1){
					//Send green
					//need to check which is normal color
					Target_Data = target_temp_green;
					//Do the decision on micro800
				}

				if(target_temp>0 && target_temp_green>0 &&round==2){
					//Send red
					Target_Data = target_temp;
					//Do the decision on micro800
				}
				else if(target_temp==0 && target_temp_green>0 &&round==2){
					Target_Data = target_temp_green;
				}
				else{
					Target_Data = target_temp_green;
				}
			}

			return mat;
		}
	}

    public String getColorName(double r, double g, double b){
        
        String ColorName = null;
    	
	    //Mode ranges from Colors
    	// We estimate from Hue, instead of the value ... So we ranges
    	// http://en.wikipedia.org/wiki/Hue
        //Black
        if(r<100 && g<100 && b<100){
    		if(r < 20.0 && g < 20.0 && b < 20.0){
              	ColorName = "Tone Black";
            }
    		else{
    			ColorName = "LED not ON";
    		}
    	}
        //White
        else if(r > 140.0 && g > 140.0 && b > 140.0){
    		if(r > 200.0 && g > 200.0 && b > 200.0){
    			ColorName = "White Pure";
    		}else{
    			ColorName = "Tone White";
    		}
    	}
        else if(r >= 180){
    		ColorName = "Primary Red";
    	}
        else if(g >= 180){
    		ColorName = "Primary Green";
    	}
        else if(b >= 180){
    		ColorName = "Primary Blue";
    	}
    	//Red
        else if(r >= g && g >= b){
    		ColorName = "Tone Red";
    	}
    	//Yellow
        else if(g > r && r >= b){
    		ColorName = "Tone Yellow";
    	}
    	//Green
        else if(g >= b && b > r){
    		ColorName = "Tone Green";
    	}
    	//Cyan
        else if(b > g && g > r){
    		ColorName = "Tone Cyan";
    	}
    	//Blue
        else if(b > r && r >= g){
    		ColorName = "Tone Blue";
    	}
    	//Magenta
        else if(r >= b && b > g){
    		ColorName = "Tone Magenta";
    	}    	
     
        
        else{
        	ColorName = "Error";
        }
    	
    	return ColorName;
    }
    
    public void MeasureScreenSize() {
        //Camera mCamera = Camera.open(-1);
        //Camera.Parameters params = mCamera.getParameters();
        //Log.d("ApplicationTagName", "Display width in px is " + params.getPreviewSize().height );
        //hightCamara = params.getPreviewSize().height;
    }
    
}