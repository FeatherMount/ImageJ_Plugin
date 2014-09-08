import Jama.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;
import java.util.ArrayList;
import ij.measure.*;
import ij.io.*;
public class Pombe_Measurer implements PlugInFilter
{
    ImagePlus imp;
    private static Double pixelWidth = null;
    public int setup(String arg, ImagePlus imp) 
    {
        this.imp = imp;
	return DOES_ALL;
    }

    public void run(ImageProcessor ip) 
    {
        try{
        LogStream.redirectSystem();
        IJ.redirectErrorMessages(false);
        Roi roi = imp.getRoi();
        if (roi == null || !roi.isArea())
        {
            IJ.error("Pombe Measurer","Area selection required.");
            return;
        }
        FloatPolygon fp = roi.getInterpolatedPolygon();

        // preparing matrix
        double[][] values = new double[fp.npoints][2];
        double sumX = 0;
        double sumY = 0;
        for (int i = 0; i < fp.npoints; i++)
        {            
            // array of x points
            values[i][0] = (double)fp.xpoints[i];
            sumX += fp.xpoints[i];
            // array of y points
            values[i][1] = (double)fp.ypoints[i];    
            sumY += fp.ypoints[i];
        }
        
        double meanX = sumX / fp.npoints;
        double meanY = sumY / fp.npoints;
        Matrix data = new Matrix(values);
        
        Matrix meanData = new Matrix(fp.npoints,2);
        for (int i = 0; i < fp.npoints; i++)
        {
            meanData.set(i,0,meanX);
            meanData.set(i,1,meanY);
        }
        Matrix nData = data.minus(meanData);
        Matrix cov = nData.transpose().times(nData);
        EigenvalueDecomposition ed = cov.eig();
        double[] ev = ed.getRealEigenvalues();
        
        Matrix eigenvector = ed.getV();
        // [x0 y0] gives long axis direction. unit vector
        double x0 = eigenvector.get(0,1);
        double y0 = eigenvector.get(1,1);
        
        // rotation matrix
        Matrix rMatrix = new Matrix(2,2);
        rMatrix.set(0,0,x0);
        rMatrix.set(0,1,y0);
        rMatrix.set(1,0,-y0);
        rMatrix.set(1,1,x0);
        // rotate normalized data. unnecessary to put back to 
        // original place in 2D
        Matrix rotatedCell = rMatrix.times(nData.transpose()).transpose();
        // this flatCell is the same with rotatedCell. 
        // Just in double[][] format. 
        double[][] flatCell = rotatedCell.getArray();
        
        
        double[] flatX = new double[flatCell.length]; 
        double[] flatY = new double[flatCell.length];
        for (int i = 0; i < flatCell.length; i++)
        {
            flatX[i] = flatCell[i][0];
            flatY[i] = flatCell[i][1];
        }
        if (pixelWidth == null)
        {
            GenericDialog gd = new GenericDialog("Enter pixel length");
            gd.addNumericField("Pixel Width: ", 1, 5);
            gd.showDialog();
            if (gd.wasCanceled()) pixelWidth = 1.;

            pixelWidth = (Double) gd.getNextNumber();
        }       
        
        // now calculate the length
        double cellLength = pixelWidth*(max(flatX) - min(flatX));
        // now calculate the width at fattest region
        double cellWidth = pixelWidth*(max(flatY) - min(flatY));
        // now calculate the surface area and the volume
        double cellVolume = 0;
        double cellSurfaceArea = 0;
        double stepSize = 1;
        double r1, r2 = 0.;
        for (double x = min(flatX); x <= max(flatX); x = x + stepSize)
        {
            // get indices for cell disk
            ArrayList<Integer> indices = search(flatX, x, 0.5);
            double[] yValues = new double[indices.size()];
            int yIndex = 0;
            for (int index : indices)
            {
                yValues[yIndex] = flatY[index];
                yIndex++;
            }
            // get radius of the disk r1
            r1 = (max(yValues) - min(yValues)) / 2.;
            // need to make sure that r1 is greater than r2
            boolean isSwapped = false;
            if (r1 < r2) 
            {
                isSwapped = !isSwapped;
                double temp = r1;
                r1 = r2;
                r2 = temp;
            }
            if (r1 != r2)
            {
                double x_imagine = stepSize * r2 / (r1 - r2);
                double l1 = Math.sqrt(r1*r1 + (stepSize + x_imagine) 
                        * (stepSize + x_imagine));
                double l2 = Math.sqrt(x_imagine * x_imagine + r2 * r2);
                cellSurfaceArea += Math.PI * (r1*l1 - r2*l2);
                cellVolume +=  1./3. * (stepSize + x_imagine) * (Math.PI * r1 * r1) 
                    - 1./3. * x_imagine * (Math.PI * r2 * r2);
            }
            if (r1 == r2)
            {
                cellSurfaceArea += Math.PI * r1 * 2. * stepSize;
                cellVolume += Math.PI * r1 * r1 * stepSize;
            }
            // r2 stores the previous slice in the next loop            
            if (!isSwapped) r2 = r1;
        }
        // convert pixel scale to physical scale
        cellVolume = cellVolume * pixelWidth * pixelWidth * pixelWidth * stepSize;
        cellSurfaceArea = cellSurfaceArea * pixelWidth * pixelWidth * stepSize;

        ResultsTable rt = Analyzer.getResultsTable();
        if (rt == null) 
        {
            rt = new ResultsTable();
            Analyzer.setResultsTable(rt);
        }
        rt.incrementCounter(); 
        rt.addValue("Length", cellLength);
        rt.addValue("Width", cellWidth);
        rt.addValue("Surface Area", cellSurfaceArea);
        rt.addValue("Volume", cellVolume);

        rt.show("Results");

        }catch(Exception e){
            imp.unlock();
        }
    }

    private static double max(double[] arr)
    {
        double temp = Double.MIN_VALUE;
        for (int i = 0; i < arr.length; i++)
        {
            if (temp < arr[i])
                temp = arr[i];
        }
        return temp;
    }

    private static double min(double[] arr)
    {
        double temp = Double.MAX_VALUE;
        for (int i = 0; i < arr.length; i++)
        {
            if (temp > arr[i])
                temp = arr[i];
        }
        return temp;
    }
    
    // search in an array and return the index
    // matching means value differs less than half pixel width
    private static ArrayList<Integer> search(double[] arr, double key, double tolerance)
    {
        ArrayList<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i < arr.length; i++)
            if (Math.abs(arr[i] - key) < tolerance)
                result.add(i);
        return result;
    }
}
