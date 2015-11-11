package zy.gridcolor;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
//import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
//import android.widget.EditText;
//import android.widget.TextView;
import android.widget.Toast;
import net.wimpi.modbus.usbserial.SerialParameters;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;

public class OpenCVMainActivity extends Activity implements CvCameraViewListener2 {
    
    private static final String  TAG              = "gridcolor::Activity";
    
    private static final int[][] POS_MASK = {
                                                {0x0001, 0x0002, 0x0004, 0x0008},
                                                {0x0010, 0x0020, 0x0040, 0x0080},
                                                {0x0100, 0x0200, 0x0400, 0x0800},
                                                {0x1000, 0x2000, 0x4000, 0x8000}
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
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        
        Log.i(TAG, "opencv_main_activity");
        setContentView(R.layout.opencv_main_activity);
        
        Log.i(TAG, "camara java");
        camara = (CameraBridgeViewBase)findViewById(R.id.camara_java);

    	camara.setVisibility(SurfaceView.VISIBLE);
    	camara.setCvCameraViewListener(this);
    	
    	//Measure Screen Size
    	MeasureScreenSize();
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
    	
    	if(GrayMode){
    		//Mode white and black
    		return frame.gray();
    	}else{
	    	// Mat, then work in the frame pixels
	    	Mat mat = frame.rgba();
	    	
	    	double box_starting_point_width = 0;//(widthCamara-hightCamara)/2;
	    	double box_ending_point_width = hightCamara;//(widthCamara+hightCamara)/2;
	    	double[] box_cell_color = {255, 255, 255, 255};
	    	Imgproc.rectangle(mat, new Point(box_starting_point_width, 0), new Point(box_ending_point_width, hightCamara), new Scalar(box_cell_color[0], box_cell_color[1], box_cell_color[2], box_cell_color[3]), 5); //When painting, we use RGBA
	    	//Lines horizontal
            //Imgproc.line(mat, new Point(box_starting_point_width, cell_length), new Point(box_ending_point_width, cell_length), new Scalar(box_cell_color[0], box_cell_color[1], box_cell_color[2],box_cell_color[3]), 2, 1, 0);
            //Imgproc.line(mat, new Point(box_starting_point_width, cell_length*2), new Point(box_ending_point_width, cell_length*2), new Scalar(box_cell_color[0], box_cell_color[1], box_cell_color[2],box_cell_color[3]), 2, 1, 0);
            //Imgproc.line(mat, new Point(box_starting_point_width, cell_length*3), new Point(box_ending_point_width, cell_length*3), new Scalar(box_cell_color[0], box_cell_color[1], box_cell_color[2],box_cell_color[3]), 2, 1, 0);
            //Lines Vertical
            //Imgproc.line(mat, new Point(box_starting_point_width+cell_length, 0), new Point(box_starting_point_width+cell_length, hightCamara), new Scalar(box_cell_color[0], box_cell_color[1], box_cell_color[2],box_cell_color[3]), 2, 1, 0);
            //Imgproc.line(mat, new Point(box_starting_point_width+cell_length*2, 0), new Point(box_starting_point_width+cell_length*2, hightCamara), new Scalar(box_cell_color[0], box_cell_color[1], box_cell_color[2],box_cell_color[3]), 2, 1, 0);
            //Imgproc.line(mat, new Point(box_starting_point_width+cell_length*3, 0), new Point(box_starting_point_width+cell_length*3, hightCamara), new Scalar(box_cell_color[0], box_cell_color[1], box_cell_color[2],box_cell_color[3]), 2, 1, 0);
	    	
	    	double scale = hightCamara/49.5;
	    	double cell_width = scale*3.5;
	    	double cell_hight = scale*1.3;
	    	double left_blank_space = scale*9;
	    	double botom_blank_space = scale*10;
	    	double width_seperate_space = scale*6;
	    	double height_seperate_space = scale*7.8;
	    	//Imgproc.circle(mat, new Point(0, 0), 50, new Scalar(255, 0, 0), -1);
	    	Imgproc.rectangle(mat, new Point(0, 0), new Point(250, 50), new Scalar(255, 255, 255, 255), -1); //When painting, we use RGBA
	    	
	    	int target_temp = 0;
	    	for (int i=0;i<4;i++)
	    	{
	    	    for(int j=0;j<4;j++)
	    	    {
	    	        double cell_left_top_corner_x = box_starting_point_width+left_blank_space+(cell_width+width_seperate_space)*i;
	    	        double cell_left_top_corner_y = hightCamara-(botom_blank_space+cell_hight+(cell_hight+height_seperate_space)*j);
	    	        double cell_right_botom_corner_x = box_starting_point_width+left_blank_space+cell_width+(cell_width+width_seperate_space)*i;
	    	        double cell_right_botom_corner_y = hightCamara-(botom_blank_space+(cell_hight+height_seperate_space)*j);
	    	        Imgproc.rectangle(mat, new Point(cell_left_top_corner_x, cell_left_top_corner_y), new Point(cell_right_botom_corner_x, cell_right_botom_corner_y), new Scalar(box_cell_color[0], box_cell_color[1], box_cell_color[2], box_cell_color[3]), 2); //When painting, we use RGBA
	    	        int cell_central_x = (int)(cell_left_top_corner_x+cell_width/2);
	    	        int cell_central_y = (int)(cell_left_top_corner_y+cell_hight/2);
	    	        //TEXT position
	    	        double text_slot_hight = hightCamara/32;
	    	        //Retrieve the color of the central pixel
	    	        double[] CellColor = mat.get(cell_central_y, cell_central_x);
	    	        if(CellColor != null)
	    	        {
    	    	        //The inverse color, to paint the circle and always see it
    	    	        //double[] CellColorInverse = { 255 - CellColor[0], 255 - CellColor[1], 255 - CellColor[2], 255};
    	                //Circle
    	    	        //Imgproc.circle(mat, new Point(cell_central_x, cell_central_y), (int)(cell_hight/2), new Scalar(255, 255, 255), 2);
    	    	        //Imgproc.circle(mat, new Point(cell_central_x, cell_central_y), (int)(cell_hight/2), new Scalar(CellColorInverse[0], CellColorInverse[1], CellColorInverse[2]), 2);
    	    	        //TEXT
    	                //Text generated in each frame with color in BGR (float)
    	                //Yes, BGR, OpenCV handles colors like Blue Green Red, not Red Green Blue
    	    	        //Mark Cell
    	    	        String text_cell = "("+i+","+j+")";
    	    	        Imgproc.putText(mat, text_cell, new Point(cell_right_botom_corner_x, cell_right_botom_corner_y), 3, 0.75, new Scalar(255, 255, 255, 255), 1);
    	    	        //RGB Info
    	                String text = "RGB: (" + i + "," + j + ") " + CellColor[0] + " " + CellColor[1] + " " + CellColor[2];
    	                Imgproc.putText(mat, text, new Point(box_ending_point_width, text_slot_hight*(2*(i*4+j)+1)), 3, 0.75, new Scalar(255, 0, 0, 255), 2);
    	                //text Color Name
                        String ColorName = getColorName(CellColor[0], CellColor[1], CellColor[2]);
                        if(RunFlag)
                        {
                            if (ColorName == "Tone Green")
                            {
                                target_temp |= POS_MASK[i][j];
                            }
                        }
    	                Imgproc.putText(mat, ColorName, new Point(box_ending_point_width, text_slot_hight*(2*(i*4+j)+2)), 3, 0.75, new Scalar(255, 0, 0, 255), 2);
	    	        }
	    	    }
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
    	//Red
    	if(r >= g && g >= b){
    		ColorName = "Tone Red";
    	}
    	//Yellow
    	if(g > r && r >= b){
    		ColorName = "Tone Yellow";
    	}
    	//Green
    	if(g >= b && b > r){
    		ColorName = "Tone Green";
    	}
    	//Cyan
    	if(b > g && g > r){
    		ColorName = "Tone Cyan";
    	}
    	//Blue
    	if(b > r && r >= g){
    		ColorName = "Tone Blue";
    	}
    	//Magenta
    	if(r >= b && b > g){
    		ColorName = "Tone Magenta";
    	}
    	//Black
    	if(r < 10.0 && g < 10.0 && b < 10.0){
    		ColorName = "Tone Black";
    	}
    	//White
    	if(r > 140.0 && g > 140.0 && b > 140.0){
    		if(r > 200.0 && g > 200.0 && b > 200.0){
    			ColorName = "White Pure";
    		}else{
    			ColorName = "Tone White";
    		}
    	}
    	return ColorName;
    }
    
    public void MeasureScreenSize() {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        //widthCamara = displaymetrics.widthPixels;
        hightCamara = displaymetrics.heightPixels;
    }
}