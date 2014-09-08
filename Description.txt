Description of Pombe Measurer

Author: Zhou Zhou
Affiliatoin: Laboratory of Fred Chang at Columbia University

Overview

This plugin of ImageJ measuers the length, width, surface area, and the volume of a rod shaped cell or sperical spores given that the user draw the outline of the cell as an ROI. 

Requirement

This plugin works with ImageJ and it requires the JAMA package to carry out the matrix manipulation. 

Installation

Download the .class file into the plugin folder of your ImageJ installation folder. Also make sure that JAMA.jar is in the same plugin folder. Start or restart ImageJ if you had it open. 

Usage

Draw the outline of the cell using polygon tools in ImageJ. The input of the plugin needs an area selection. Click Plugin -- Pombe Measurer and it will pop up to let you input the pixel length. This window will only pop up once. If you need to change the pixel length later on, restart ImageJ. Once you have entered the pixel length, click OK. 

Details of Algorithm

When processing rod-shaped cellular images, one time-consuming task is to rotate the long axis of the cell in the image plane to be horizontal. In Pombe Measurer, the actual coordinates of the outline of the cell are used and Principle Component Analysis is performed to find the long axis of the cell. Then a rotation matrix is calculated from this angle to rotate the image. No manual work is needed after the plugin launches.  

The cell is then sliced vertically into thin slices (typically with one pixel width). The surface area and the total volume of the cell are approximated by aggregating the surface area and the volume of the cone frusta.   

Issues

Because of resolution limit and the algorithm cannot approximate the surface area and volume measurements using thin slices under pixel resolution.

The worst performance is with perfect speres in that appximation is least accurate. For example, a 100 pixel diameter sphere will have a 4.6% underestimation in surface area. When it is only 30 pixels in diameter, this underestimation is about 9%. This may be improved in the next version. 

The surface area measurement of long rods is more accurate and the measurement of volume is accurate in general (error < 1%) with worst case performance in perfect small spheres of less than 5% underestimation. 

