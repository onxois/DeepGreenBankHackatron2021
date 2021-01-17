package de.stash.bank2021;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.plot.CategoryPlot; 
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import gui.NoCloseFrame;
import gui.tables.BasicColumn;
import gui.tables.ButtonColumn;
import gui.tables.ColoredSpinnerColumn;
import gui.tables.ComboBoxColumn;
import gui.tables.DateTimeColumn;
import gui.tables.IdColumn;
import gui.tables.SpinnerColumn;
import gui.tables.TableFramework;
import gui.tables.TableLine;
import io.SFXTools;
import io.TextUtil;

public class Main extends SFXTools {
	private TableFramework viirsTable = null;
	private JPanel viirsMaster = new JPanel();

	private TableFramework viirsDev1Table = null;
	private JPanel viirsDev1Master = new JPanel();

	private TableFramework traseSoyTable = null;
	private JPanel traseSoyMaster = new JPanel();

	private TableFramework dgiTable = null;
	private JPanel dgiMaster = new JPanel();

	private TableFramework soyPriceTable = null;
	private JPanel soyPriceMaster = new JPanel();
	
	public final static boolean ALLOW_SORTING_TABLES = true;
	private String folder = null;
	private String ZIPfilename=null;
	private NoCloseFrame jTreeframe = null;
	private TimeZone tz = TimeZone.getTimeZone("UTC");
	
	private final String separator = ",";
	public Main() {
		this("data", null);
	}
	public Main(String folder, String ZIPfilename) {
		this.folder = folder;
		this.ZIPfilename = ZIPfilename;
		ps = System.out;
		psErr = System.err;

	}
	
	public static void main(String[] args) {
		Main fd = new Main();
		
		fd.run();
	}

	public void run() {
		JTabbedPane positionsFrameTabbedPane = initTabbedPane();
		doCalculation();
		positionsFrameTabbedPane.add("FIRMS days chart",createFireChart());
		positionsFrameTabbedPane.add("FIRMS chart smoothed %",createSmoothed100PercFireChart());
		
		positionsFrameTabbedPane.add("Soy chart",createSoyChart());
		positionsFrameTabbedPane.add("Soy chart %",create100PercSoyChart());
		positionsFrameTabbedPane.add("DeepGreenIndex %",createDeepGreenIndexChart());
		initFrame(positionsFrameTabbedPane);		
	}
	
	private void initFrame(JTabbedPane aPane) {
        jTreeframe = new NoCloseFrame(this.getClass().getName(), "Do you want to close all windows?"){
        	@Override
            public void yesClose(){
        		askMessage = false;
        		saveAll();
        		System.exit(0);
            }
        };
         
        jTreeframe.setContentPane(aPane);
         
    	GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    	int width = gd.getDisplayMode().getWidth();
    	int height = gd.getDisplayMode().getHeight();
    	
    	jTreeframe.setPreferredSize(new Dimension(Math.min(2400,width)-50,Math.min(600,height/3*2)-50));
        
        //Display the window.
        jTreeframe.pack();
        jTreeframe.setVisible(true);
	}
	public void saveAll() {
		// not needed, raw data
		// if (viirsTable != null) viirsTable.saveTable();
		if (viirsDev1Table != null) viirsDev1Table.saveTable();
		if (traseSoyTable != null) traseSoyTable.saveTable();
		if (dgiTable != null) dgiTable.saveTable();
		
	}

	public JTabbedPane initTabbedPane() {
		// final String ZIP_FILE = folder+File.separator+ZIPfilename;
		JTabbedPane positionsFrameTabbedPane = new JTabbedPane();
		
		/*
		// raw viirs
		Locale.setDefault(new Locale ("en", "US"));
		TableLine plannedTL = new TableLine(false, false, separator, true);
		createViirsTable(plannedTL);    	
		viirsTable = new TableFramework("viirs-snpp_2019_Brazil.csv", viirsMaster, false, folder+File.separator+"firms"+File.separator+"viirs-snpp_2019_Brazil.csv", 
				true,false, 
				plannedTL, Locale.ENGLISH, true, true, false,false, false, false, null, true, true, true,
				ALLOW_SORTING_TABLES, null);
		viirsTable.readTable();
		positionsFrameTabbedPane.add("raw",viirsMaster );
		*/
		p("generating viirs table");
		// viirs dev1
		TableLine dev1TL = new TableLine(false, false, ",", false);
		createViirsFireTable(dev1TL);    	
		viirsDev1Table = new TableFramework("viirs-snpp_2019_Brazil_DEV1.csv", viirsDev1Master, false, folder+File.separator+"firms"+File.separator+"viirs-snpp_2019_Brazil_DEV1.csv", 
				true, true, 
				dev1TL, Locale.ENGLISH, true, true, false,false, false, false, null, true, true, true,
				ALLOW_SORTING_TABLES, null);
		// viirsDev1Table.clearTable();
		positionsFrameTabbedPane.add("fire days",viirsDev1Master );
	
		p("generating trase soy");

		TableLine soyTL = new TableLine(false, false, ",", false);
		createTraseSoyTable(soyTL);    	
		traseSoyTable = new TableFramework("trase_soy.csv", traseSoyMaster, false, folder+File.separator+"trase_soy.csv", 
				true, true, 
				soyTL, Locale.ENGLISH, true, true, false,false, false, false, null, true, true, true,
				ALLOW_SORTING_TABLES, null);
		// viirsDev1Table.clearTable();
		positionsFrameTabbedPane.add("Trase Soy",traseSoyMaster );
		
		p("generating DeepGreenIndex DGI");
		TableLine dgiTL = new TableLine(false, false);
		createDeepGreenIndexTable(dgiTL);    	
		dgiTable = new TableFramework("DGI", dgiMaster, false, folder+File.separator+"dgi.csv", 
				true, true, 
				dgiTL, Locale.ENGLISH, true, true, false,false, false, false, null, true, true, true,
				ALLOW_SORTING_TABLES, null);
		dgiTable.clearTable();
		positionsFrameTabbedPane.add("DeepGreenIndex",dgiMaster );

		p("generating soy price table");
		TableLine soyPriceTL = new TableLine(false, false, ",", false);
		createSoyBeanPriceTable(soyPriceTL);    	
		soyPriceTable = new TableFramework("DGI", soyPriceMaster, false, folder+File.separator+"macrotrends.net"+File.separator+"soybean-prices-historical-chart-data_updated.csv", 
				true, true, 
				soyPriceTL, Locale.ENGLISH, true, true, false,false, false, false, null, true, true, true,
				ALLOW_SORTING_TABLES, null);
		
		positionsFrameTabbedPane.add("Soy price",soyPriceMaster );
		
		
		return positionsFrameTabbedPane;
	}
	
	private void createViirsTable(TableLine rtl){
		rtl.add(new BasicColumn("latitude","", 50,1000, true));
		rtl.add(new BasicColumn("longitude","", 50,1000, true));
		rtl.add(new BasicColumn("bright_ti4","", 50,1000, true));
		rtl.add(new BasicColumn("scan","", 50,1000, true));
		rtl.add(new BasicColumn("track","", 50,1000, true));
		rtl.add(new DateTimeColumn("acq_date", null, 150,170, true, "acq_date", TimeZone.getTimeZone("UTC"), "yyyy-MM-dd", false, true));
		rtl.add(new BasicColumn("acq_time","", 50,1000, true));
		rtl.add(new BasicColumn("satellite","", 50,1000, true));
		rtl.add(new BasicColumn("instrument","", 50,1000, true));
		rtl.add(new BasicColumn("confidence","", 50,1000, true));
		rtl.add(new BasicColumn("version","", 50,1000, true));
		rtl.add(new BasicColumn("bright_ti5","", 50,1000, true));
		rtl.add(new BasicColumn("frp","", 50,1000, true));
		rtl.add(new BasicColumn("daynight","", 50,1000, true));
		rtl.add(new BasicColumn("type","", 50,1000, true));
	}
	
	private void createViirsFireTable(TableLine rtl){
		rtl.add(new DateTimeColumn("acq_date", null, 150,170, true, "acq_date", TimeZone.getTimeZone("UTC"), "yyyy-MM-dd", false, true));
		rtl.add(new BasicColumn("fires","", 150,1000, true));
	}

	private void createTraseSoyTable(TableLine rtl){
		rtl.add(new DateTimeColumn("year", null, 150,170, true, "year", TimeZone.getTimeZone("UTC"), "yyyy-MM-dd", false, true));
		rtl.add(new BasicColumn("SOY_EQUIVALENT_TONNES","", 50,1000, true));
		rtl.add(new BasicColumn("LAND_USE_HA","", 50,1000, true));
		rtl.add(new BasicColumn("TERRITORIAL_DEFORESTATION_RISK_HA","", 50,1000, true));
	}

	private void createDeepGreenIndexTable(TableLine rtl){
		rtl.add(new DateTimeColumn("date", null, 150,170, true, "date", TimeZone.getTimeZone("UTC"), "yyyy-MM-dd", false, true));
		rtl.add(new BasicColumn("fires %","", 150,1000, true));
		rtl.add(new BasicColumn("SOY_EQUIVALENT_TONNES %","", 150,1000, true));
		rtl.add(new BasicColumn("LAND_USE_HA %","", 150,1000, true));
		rtl.add(new BasicColumn("TERRITORIAL_DEFORESTATION_RISK_HA %","", 150,1000, true));		
		rtl.add(new BasicColumn("DeepGreenIndex","", 300,1000, true));
		rtl.add(new BasicColumn("Soy price %","", 300,1000, true));
	}
	
	private void createSoyBeanPriceTable(TableLine rtl){
		rtl.add(new DateTimeColumn("date", null, 150,170, true, "date", TimeZone.getTimeZone("UTC"), "yyyy-MM-dd", false, true));
		rtl.add(new BasicColumn("price","", 50,1000, true));
	}


	
	
	private void doCalculation() {
		if (viirsDev1Table.getRowCount() == 0) {
			readViirsLinesFromDisk(folder+File.separator+"firms"+File.separator+"fire_archive_V1_11619.csv");
		}		
		if (traseSoyTable.getRowCount() == 0) {
			readSoyLinesFromDisk(folder+File.separator+"trase"+File.separator+"BRAZIL_SOY_2.5.1_pc.csv");
		}		
		
		
		/*
		Date last = null;
		int counter = 0;
		for (TableLine tl : viirsTable.getTableLineList()) {
			Date d = tl.getDate("acq_date");	
			if (last == null) last = d;
			
			if (!last.equals(d)) {
				viirsDev1Table.createStashLineAddLineAtEnd(new Object[] {last, counter });
				last = d;
				counter = 1;
				continue;
			}
			counter++;			
		}
		if (counter > 0)  viirsDev1Table.createStashLineAddLineAtEnd(new Object[] {last, counter });
		*/
	}
	
	// https://www.codeproject.com/Articles/650480/Introduction-to-JFreeChart
	private ChartPanel createFireChart() {
		int counter = 0;
		DefaultCategoryDataset objDataset = new DefaultCategoryDataset();
		for (TableLine tl : viirsDev1Table.getTableLineList()) {
			
			Calendar d = tl.getUTCCalendarDate("acq_date");
			String dStr = TextUtil.getShortGMTString(d);
			if (d == null) continue;
			double value = tl.getDoubleValue("fires");
			// p("value = "+value+", date = "+TextUtil.getGMTExcelString(d));
			objDataset.setValue(value,"fires",dStr); 
			/*
			if (counter%100!=0) {
				objDataset.setValue(value,"fires",""); 
			}
			else objDataset.setValue(value,"fires",TextUtil.getGMTExcelString(d));
			*/
		}
		JFreeChart jfreechart = ChartFactory.createLineChart("Fire chart", "time", "fires", objDataset);


        // JFreeChart jfreechart = ChartFactory.createXYLineChart("GUI Artistical look", "Time", "fires", xydataset, PlotOrientation.VERTICAL, true, true, false);
        jfreechart.setBackgroundPaint(new Color( 161,170,186));
        CategoryPlot plot = (CategoryPlot) jfreechart.getPlot();
        plot.getRenderer().setSeriesStroke(0, new BasicStroke(2.0f));
        plot.getDomainAxis().setVisible(false);
        ChartPanel chartpanel = new ChartPanel(jfreechart); 
        return chartpanel;		
	}
	private ChartPanel createSmoothed100PercFireChart() {
		double growth = 100D;
		double newgrowth = 100D;
		int myFirstYear = 0;
		double oldValue= -999999D;
		double[] firstYear = new double[367];
		/*
		for (int t = 0; t<firstYear.length; t++) {
			firstYear[t] = 100D;
		}
		*/
		
		DefaultCategoryDataset objDataset = new DefaultCategoryDataset();
		int currentYear = 0;
		double oldGrowth = 0D;
		for (TableLine tl : viirsDev1Table.getTableLineList()) {
			
			Calendar d = tl.getUTCCalendarDate("acq_date");
			
			String dStr = TextUtil.getShortGMTString(d);
			if (d == null) continue;
			
			int checkYear = d.get(Calendar.YEAR);
			int day = d.get(Calendar.DAY_OF_YEAR);
			double value = tl.getDoubleValue("fires");
			if (oldValue== -999999D) {
				currentYear = checkYear;
				oldValue = value;
				growth = 100D;
				myFirstYear = checkYear;
			}			
			growth = ((value / oldValue)-1D)*0.001 ;
			
			// p("value = "+value+", oldValue = "+oldValue+", growth = "+growth+", date = "+TextUtil.getGMTString(d)+", checkYear = "+checkYear+", myFirstYear = "+myFirstYear);
			
			if (checkYear == myFirstYear) {
				firstYear[day] = growth;

			}
			else {
				if (firstYear[day]!=0D) {
					newgrowth *= 1D+(growth+firstYear[day])/2D; // (growth-firstYear[day])/oldValue;		
				}
				else firstYear[day] = growth;
				double before = firstYear[day]; 
				firstYear[day] = (growth+firstYear[day])/2D;
				newgrowth *= 1D+firstYear[day]; // (growth-firstYear[day])/oldValue;
				// p("set value growth = "+newgrowth+", dStr = "+dStr+", firstYear[day]= "+firstYear[day]+", growth = "+growth+", before = "+before);
				objDataset.setValue(newgrowth,"value smoothed %, start = 100%",dStr);
				dgiTable.createStashLineAddLineAtEnd(new Object[] {new Date(d.getTimeInMillis()),firstYear[day], null, null, null, null  });

			}
			
			// p("value = "+value+", date = "+TextUtil.getGMTExcelString(d));
			 
			oldValue = value;
			oldGrowth = growth;
		}
		JFreeChart jfreechart = ChartFactory.createLineChart("FIRMS chart seasonal corrected, smoothed, %", "time", "%", objDataset);


        // JFreeChart jfreechart = ChartFactory.createXYLineChart("GUI Artistical look", "Time", "fires", xydataset, PlotOrientation.VERTICAL, true, true, false);
        jfreechart.setBackgroundPaint(new Color( 161,170,186));
        CategoryPlot plot = (CategoryPlot) jfreechart.getPlot();
        plot.getRenderer().setSeriesStroke(0, new BasicStroke(2.0f));
        plot.getDomainAxis().setVisible(false);
        ChartPanel chartpanel = new ChartPanel(jfreechart); 
        return chartpanel;		
	}
	
	private ChartPanel createSoyChart() {
		DefaultCategoryDataset objDataset = new DefaultCategoryDataset();
		for (TableLine tl : traseSoyTable.getTableLineList()) {
			
			Calendar d = tl.getUTCCalendarDate("year");
			String dStr = Integer.toString(d.get(Calendar.YEAR));
			if (d == null) continue;
			double soyton = tl.getDoubleValue("SOY_EQUIVALENT_TONNES");
			double landUse = tl.getDoubleValue("LAND_USE_HA");
			double defRisk = tl.getDoubleValue("TERRITORIAL_DEFORESTATION_RISK_HA");
			// p("value = "+value+", date = "+TextUtil.getGMTExcelString(d));
			objDataset.setValue(soyton,"SOY_EQUIVALENT_TONNES %, start = 100%",dStr); 
			objDataset.setValue(landUse,"LAND_USE_HA %",dStr); 
			objDataset.setValue(defRisk,"TERRITORIAL_DEFORESTATION_RISK_HA %",dStr); 
		}
		JFreeChart jfreechart = ChartFactory.createLineChart("Land use & deforestation risk chart", "time", "units", objDataset);


        // JFreeChart jfreechart = ChartFactory.createXYLineChart("GUI Artistical look", "Time", "fires", xydataset, PlotOrientation.VERTICAL, true, true, false);
        jfreechart.setBackgroundPaint(new Color( 161,170,186));
        CategoryPlot plot = (CategoryPlot) jfreechart.getPlot();
        plot.getRenderer().setSeriesStroke(0, new BasicStroke(2.0f));
        plot.getRenderer().setSeriesStroke(1, new BasicStroke(2.0f));
        plot.getRenderer().setSeriesStroke(2, new BasicStroke(2.0f));
        plot.getDomainAxis().setVisible(true);
        ChartPanel chartpanel = new ChartPanel(jfreechart); 
        return chartpanel;
		
	}
	
	private ChartPanel create100PercSoyChart() {
		
		
		
		DefaultCategoryDataset objDataset = new DefaultCategoryDataset();
		boolean first = true;
		double soyton = 100D;
		double landUse = 100D;
		double defRisk = 100D;
		double soyton100 = 100D;
		double landUse100 = 100D;
		double defRisk100 = 100D;
		double soytonChange = 0D;
		double landUseChange = 0D;
		double defRiskChange = 0D;
		
		double dgi = 0D;
		
		for (TableLine tl : traseSoyTable.getTableLineList()) {
			
			Calendar d = tl.getUTCCalendarDate("year");
			String dStr = Integer.toString(d.get(Calendar.YEAR));
			if (d == null) continue;

			if (first) {
				first = false;	
			}
			else {
				soytonChange = (tl.getDoubleValue("SOY_EQUIVALENT_TONNES") / soyton)-1D;
				soyton100 *= soytonChange+1D; 
				landUseChange = (tl.getDoubleValue("LAND_USE_HA")/landUse)-1D;
				landUse100 *= landUseChange+1D;
				defRiskChange = (tl.getDoubleValue("TERRITORIAL_DEFORESTATION_RISK_HA")/defRisk)-1D;
				defRisk100 *= defRiskChange+1D;
				
			}
			// p("value = "+value+", date = "+TextUtil.getGMTExcelString(d));
			objDataset.setValue(soyton100,"SOY_EQUIVALENT_TONNES",dStr); 
			objDataset.setValue(landUse100,"LAND_USE_HA",dStr); 
			objDataset.setValue(defRisk100,"TERRITORIAL_DEFORESTATION_RISK_HA",dStr);
			double fireImpact = 100D;

			
			double soyPriceIndex = 100D;
			double soyOldPrice = 100D;
			
			for (TableLine dgid : dgiTable.getTableLineList()) {
				Calendar day = dgid.getUTCCalendarDate("date");
				// p("day = "+TextUtil.getGMTString(day)+", d = "+TextUtil.getGMTExcelString(d));
				if (day.get(Calendar.YEAR) == d.get(Calendar.YEAR)-1) {
					double fires100 = dgid.getDoubleValue("fires %"); 
					fireImpact *= (fires100/100D+1D); 
					// p("day = "+TextUtil.getGMTString(day)+", d = "+TextUtil.getGMTExcelString(d));
					dgid.setValue("SOY_EQUIVALENT_TONNES %", soytonChange*100D);
					dgid.setValue("LAND_USE_HA %", landUseChange*100D);
					dgid.setValue("TERRITORIAL_DEFORESTATION_RISK_HA %", defRiskChange*100D);
					if (dgi != 0) dgi *= fireImpact/100D * (soytonChange/365D/4D+1D) * (landUseChange/365D/4D+1D) * (defRiskChange/365D/4D+1D);
					else dgi = 100D;
					dgid.setValue("DeepGreenIndex", dgi);
					
					/*
					boolean foundOne = false;
					for (TableLine priceTL : soyPriceTable.getTableLineList()) {
						Calendar priceDay = priceTL.getUTCCalendarDate("date");
						
						p("priceDay = "+TextUtil.getGMTExcelString(priceDay)+", day = "+TextUtil.getGMTExcelString(day));
						if (priceDay.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
								priceDay.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)) {
							double soyPrice = priceTL.getDoubleValue("price");
							if (soyPrice == 0D) soyPrice = soyOldPrice;
							if (soyPrice == 0D) continue;
							soyPriceIndex *= soyPrice/soyOldPrice;							
							dgid.setValue("Soy price %", soyPriceIndex);
							foundOne = true;
							soyOldPrice = soyPrice;
							break;
						}
					}
					if (!foundOne) {
						dgid.setValue("Soy price %", soyPriceIndex);
					}
					*/									
				}
			}
			

			
			
			soyton = tl.getDoubleValue("SOY_EQUIVALENT_TONNES");
			landUse = tl.getDoubleValue("LAND_USE_HA");
			defRisk = tl.getDoubleValue("TERRITORIAL_DEFORESTATION_RISK_HA");
		}
		JFreeChart jfreechart = ChartFactory.createLineChart("Land use, soy output & deforestation risk chart, %", "time", "%", objDataset);


        // JFreeChart jfreechart = ChartFactory.createXYLineChart("GUI Artistical look", "Time", "fires", xydataset, PlotOrientation.VERTICAL, true, true, false);
        jfreechart.setBackgroundPaint(new Color( 161,170,186));
        CategoryPlot plot = (CategoryPlot) jfreechart.getPlot();
        plot.getRenderer().setSeriesStroke(0, new BasicStroke(2.0f));
        plot.getRenderer().setSeriesStroke(1, new BasicStroke(2.0f));
        plot.getRenderer().setSeriesStroke(2, new BasicStroke(2.0f));
        plot.getDomainAxis().setVisible(true);
        ChartPanel chartpanel = new ChartPanel(jfreechart); 
        return chartpanel;
		
	}
	
	private ChartPanel createDeepGreenIndexChart() {
		DefaultCategoryDataset objDataset = new DefaultCategoryDataset();
		for (TableLine tl : dgiTable.getTableLineList()) {
			Calendar day = tl.getUTCCalendarDate("date");
			String dStr = TextUtil.getShortGMTString(day);
			double dgi = tl.getDoubleValue("DeepGreenIndex");
			if (dgi == 0D) continue;
			objDataset.setValue(dgi,"DGI",dStr);		
			// objDataset.setValue(tl.getDoubleValue("Soy price %"),"Soy price",dStr);
			
		}
		
		JFreeChart jfreechart = ChartFactory.createLineChart("DeepGreenIndex, %", "time", "DGI", objDataset);


        // JFreeChart jfreechart = ChartFactory.createXYLineChart("GUI Artistical look", "Time", "fires", xydataset, PlotOrientation.VERTICAL, true, true, false);
        jfreechart.setBackgroundPaint(new Color( 161,170,186));
        CategoryPlot plot = (CategoryPlot) jfreechart.getPlot();
        plot.getRenderer().setSeriesStroke(0, new BasicStroke(2.0f));
        plot.getRenderer().setSeriesStroke(1, new BasicStroke(2.0f));
        plot.getRenderer().setSeriesStroke(2, new BasicStroke(2.0f));
        plot.getDomainAxis().setVisible(true);
        ChartPanel chartpanel = new ChartPanel(jfreechart); 
        return chartpanel;
		
	}
	
	
    private void readViirsLinesFromDisk(String fileName){
    	TimeZone tz = TimeZone.getTimeZone("UTC");
    	DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		formatter.setTimeZone(tz);
		try{
    		// ------------------
    		if (SFXTools.fileExists(fileName)){
    	    	try{	    
    	    		int rowIndex = 0;
    	    		boolean firstLine = true;
    	    		InputStream stream = null;
    	    		stream = new FileInputStream(new File(fileName));
    	    		InputStreamReader fr = new InputStreamReader(stream, "UTF-8");
    	    		
    	    		// System.out.println("filename = "+filename);
    	    		BufferedReader br = new BufferedReader(fr);
    	    		Date last = null;
    	    		int counter = 0;
    	    		
    	    		String line;
        	        while ( (line = br.readLine()) != null) {
    	        		if (firstLine){
    	        			firstLine = false;
    	        			continue;
    	        		}
    	        		String[] elements = line.split(separator);
    	        		if (elements == null || elements.length == 0){		
    	        			continue;
    	        		}
    	    			Date d = null;
    	    			try {
    	    				d = formatter.parse(elements[5]);
    	    			} catch (ParseException e) {
    	    				continue;
    	    			}
    	    			if (last == null) last = d;
    	    			
    	    			if (!last.equals(d)) {
    	    				viirsDev1Table.createStashLineAddLineAtEnd(new Object[] {last, counter });
    	    				last = d;
    	    				counter = 1;
    	    				continue;
    	    			}
    	    			counter++;		
        	        }
        	        if (counter > 0) viirsDev1Table.createStashLineAddLineAtEnd(new Object[] {last, counter });
        	        br.close();
        	        fr.close();
    	    	}
    	    	catch (Exception e){
    	    		e.printStackTrace(System.err);
    	    	}
    		}
		}
		catch (Exception e){
			e.printStackTrace(psErr);
		}		
    }

    private void readSoyLinesFromDisk(String fileName){
    	TimeZone tz = TimeZone.getTimeZone("UTC");
    	DateFormat formatter = new SimpleDateFormat("yyyy");
		formatter.setTimeZone(tz);
		try{
    		// ------------------
    		if (SFXTools.fileExists(fileName)){
    	    	try{	    
    	    		boolean firstLine = true;
    	    		InputStream stream = null;
    	    		stream = new FileInputStream(new File(fileName));
    	    		InputStreamReader fr = new InputStreamReader(stream, "UTF-8");
    	    		
    	    		// System.out.println("filename = "+filename);
    	    		BufferedReader br = new BufferedReader(fr);
    	    		
    	    		double[] soyEqu = new double[30];
    	    		double[] defRisk = new double[30];
    	    		double[] landUse = new double[30];
    	    		int minNumber = 30;
    	    		int maxNumber = 0;
    	    		
    	    		String line;
        	        while ( (line = br.readLine()) != null) {
    	        		if (firstLine){
    	        			firstLine = false;
    	        			continue;
    	        		}
    	        		String[] elements = line.split(separator);
    	        		if (elements == null || elements.length == 0){		
    	        			continue;
    	        		}
    	        		
    	        		int y = Integer.parseInt(elements[0].substring(2));
    	        		if (y < minNumber) minNumber = y;
    	        		if (y > maxNumber) maxNumber = y;
    	    			if(elements[20] != null && elements[20].length() > 0) {
    	    				try {
    	    					soyEqu[y] += Double.parseDouble(elements[20]);
    	    				}
    	    				catch(Exception e) {}
    	    			}
    	    			if(elements[17] != null && elements[17].length() > 0) {
    	    				try {
    	    					landUse[y] += Double.parseDouble(elements[17]);
    	    				}
    	    				catch(Exception e) {}
    	    			}
    	    			if(elements[15] != null && elements[15].length() > 0) {
    	    				try {
    	    					defRisk[y] += Double.parseDouble(elements[15]);
    	    				}
    	    				catch(Exception e) {}
    	    			}
        	        }
        	        for (int t = minNumber; t <= maxNumber; t++) {
        	        
		    			Date d = null;
		    			try {
		    				if (t < 10) d = formatter.parse("200"+t);
		    				else d = formatter.parse("20"+t);
		    			} catch (ParseException e) {
		    				continue;
		    			}
		    			/*
		    			Calendar cal = Calendar.getInstance(tz);
		    			cal.setTime(d);
		    			cal.add(Calendar.YEAR, 1);
		    			new Date(cal.getTimeInMillis())
		    			*/
		    			traseSoyTable.createStashLineAddLineAtEnd(new Object[] {d,soyEqu[t], landUse[t], defRisk[t] });
        	        }	

        	        
        	        br.close();
        	        fr.close();
    	    	}
    	    	catch (Exception e){
    	    		e.printStackTrace(System.err);
    	    	}
    		}
		}
		catch (Exception e){
			e.printStackTrace(psErr);
		}		
    }
	
	
}
