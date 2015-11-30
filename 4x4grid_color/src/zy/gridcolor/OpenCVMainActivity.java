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
			hightCamara = mat.height();
			double box_starting_point_width = 0;// (widthCamara-hightCamara)/2;
			double box_ending_point_width = hightCamara;// (widthCamara+hightCamara)/2;
			double[] box_cell_color = { 255, 255, 255, 255 };
			Imgproc.rectangle(mat, new Point(box_starting_point_width, 0),
					new Point(box_ending_point_width, hightCamara),
					new Scalar(box_cell_color[0], box_cell_color[1], box_cell_color[2], box_cell_color[3]), 5); // When painting, we use RGBA
			//Lines horizontal
            //Imgproc.line(mat, new Point(box_starting_point_width, cell_length), new Point(box_ending_point_width, cell_length), new Scalar(box_cell_color[0], box_cell_color[1], box_cell_color[2],box_cell_color[3]), 2, 1, 0);
            //Imgproc.line(mat, new Point(box_starting_point_width, cell_length*2), new Point(box_ending_point_width, cell_length*2), new Scalar(box_cell_color[0], box_cell_color[1], box_cell_color[2],box_cell_color[3]), 2, 1, 0);
            //Imgproc.line(mat, new Point(box_starting_point_width, cell_length*3), new Point(box_ending_point_width, cell_length*3), new Scalar(box_cell_color[0], box_cell_color[1], box_cell_color[2],box_cell_color[3]), 2, 1, 0);
            //Lines Vertical
            //Imgproc.line(mat, new Point(box_starting_point_width+cell_length, 0), new Point(box_starting_point_width+cell_length, hightCamara), new Scalar(box_cell_color[0], box_cell_color[1], box_cell_color[2],box_cell_color[3]), 2, 1, 0);
            //Imgproc.line(mat, new Point(box_starting_point_width+cell_length*2, 0), new Point(box_starting_point_width+cell_length*2, hightCamara), new Scalar(box_cell_color[0], box_cell_color[1], box_cell_color[2],box_cell_color[3]), 2, 1, 0);
            //Imgproc.line(mat, new Point(box_starting_point_width+cell_length*3, 0), new Point(box_starting_point_width+cell_length*3, hightCamara), new Scalar(box_cell_color[0], box_cell_color[1], box_cell_color[2],box_cell_color[3]), 2, 1, 0);
	    	
			double scale = hightCamara / 49.5;
			double cell_width = scale * 3.5;
			double cell_hight = scale * 1.3;
			double left_blank_space = scale * 9;
			double botom_blank_space = scale * 10;
			double width_seperate_space = scale * 6;
			double height_seperate_space = scale * 7.8;
			// Imgproc.circle(mat, new Point(0, 0), 50, new Scalar(255, 0, 0),-1);
			Imgproc.rectangle(mat, new Point(0, 0), new Point(250, 50), new Scalar(255, 255, 255, 255), -1); // When painting, we use RGBA

			int target_temp = 0;
			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 4; j++) {
					double cell_left_top_corner_x = box_starting_point_width + left_blank_space
							+ (cell_width + width_seperate_space) * i;
					double cell_left_top_corner_y = hightCamara
							- (botom_blank_space + cell_hight + (cell_hight + height_seperate_space) * j);
					double cell_right_botom_corner_x = box_starting_point_width + left_blank_space + cell_width
							+ (cell_width + width_seperate_space) * i;
					double cell_right_botom_corner_y = hightCamara
							- (botom_blank_space + (cell_hight + height_seperate_space) * j);
					Imgproc.rectangle(mat, new Point(cell_left_top_corner_x, cell_left_top_corner_y),
							new Point(cell_right_botom_corner_x, cell_right_botom_corner_y),
							new Scalar(box_cell_color[0], box_cell_color[1], box_cell_color[2], box_cell_color[3]), 2); // When painting, we use RGBA

					int cell_central_x = (int) (cell_left_top_corner_x + cell_width / 2);
					int cell_central_y = (int) (cell_left_top_corner_y + cell_hight / 2);
					// TEXT position
					double text_slot_hight = hightCamara / 32;
					// Retrieve the color of the central pixel
					double[] CellColor = mat.get(cell_central_y, cell_central_x);
					Mat roi = mat.submat(new Rect(new Point(cell_left_top_corner_x, cell_left_top_corner_y),
							new Point(cell_right_botom_corner_x, cell_right_botom_corner_y)));
					Scalar mean = Core.mean(roi);
					double[] CellColor2 = mean.val;
					if (CellColor2 != null) {
						//The inverse color, to paint the circle and always see it
    	    	        //double[] CellColorInverse = { 255 - CellColor[0], 255 - CellColor[1], 255 - CellColor[2], 255};
    	                //Circle
    	    	        //Imgproc.circle(mat, new Point(cell_central_x, cell_central_y), (int)(cell_hight/2), new Scalar(255, 255, 255), 2);
    	    	        //Imgproc.circle(mat, new Point(cell_central_x, cell_central_y), (int)(cell_hight/2), new Scalar(CellColorInverse[0], CellColorInverse[1], CellColorInverse[2]), 2);
    	    	        //TEXT
    	                //Text generated in each frame with color in BGR (float)
    	                //Yes, BGR, OpenCV handles colors like Blue Green Red, not Red Green Blue
    	    	        //Mark Cell
						String text_cell = "(" + i + "," + j + ")";
						Imgproc.putText(mat, text_cell, new Point(cell_right_botom_corner_x, cell_right_botom_corner_y),
								3, 0.75, new Scalar(255, 255, 255, 255), 1);
						// RGB Info
						String text = "RGB: (" + i + "," + j + ") " + (int) CellColor2[0] + " " + (int) CellColor2[1]
								+ " " + (int) CellColor2[2];
						Imgproc.putText(mat, text,
								new Point(box_ending_point_width, text_slot_hight * (2 * (i * 4 + j) + 1)), 3, 0.75,
								new Scalar(255, 0, 0, 255), 2);
						// text Color Name
						// String ColorName = getColorName(CellColor[0],
						// CellColor[1], CellColor[2]);
						String ColorName = getColorName(CellColor2[0], CellColor2[1], CellColor2[2]);
						POS_Color[i][j] = ColorName;
						Imgproc.putText(mat, ColorName,
								new Point(box_ending_point_width, text_slot_hight * (2 * (i * 4 + j) + 2)), 3, 0.75,
								new Scalar(255, 0, 0, 255), 2);
					}
				}
			}
			if (RunFlag) {
				/*
				 * Round 1 - Make sure at least 3 detection before firing off
				 * Location should not change if camera is fixed, only until
				 * solenoid is fired off Ensure only 1 target is detected to
				 * prevent multiple locations sent
				 */
				/*
				 * Round 2 - Has another color, if detected, priortise move to
				 * the color first
				 * 
				 */
				int detected = 0;
				boolean detected_bonus = false;
				int pos_i = 0;
				int pos_j = 0;
				int pos_bonus_i = 0;
				int pos_bonus_j = 0;
				for (int i = 0; i < 4; i++) {
					for (int j = 0; j < 4; j++) {
						if (POS_Color[i][j] == "Primary Green") {
							detected++;
							pos_i = i;
							pos_j = j;
						} else if (POS_Color[i][j] == "Primary Red") {
							detected_bonus = true;
							pos_bonus_i = i;
							pos_bonus_j = j;
						}
					}
				}
				if (detected == 1 && detected_bonus == false) {
					target_temp |= POS_MASK[pos_i][pos_j];
				} else if (detected_bonus == true) {
					target_temp |= POS_MASK[pos_bonus_i][pos_bonus_j];
				} else {
					target_temp = 0;
				}
				/*
				 * if (ColorName == "Primary Green") { if(target_temp!=0){
				 * //Detected more than once!!! Do not fire, wait for only one
				 * result } target_temp |= POS_MASK[i][j]; } if (ColorName ==
				 * "Primary Red") //Assume this is priority { //Add in check for
				 * round 2 target_temp |= POS_MASK[i][j]; //Allow overwrite } }
				 */

			}
			Target_Data = target_temp;
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