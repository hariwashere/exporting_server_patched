/**
 * @license Highcharts JS v2.3.3 (2012-11-02)
 *
 * (c) 20012-2014
 * 
 * Author: Gert Vaartjes
 *
 * License: www.highcharts.com/license
 */
package com.highcharts.export.controller;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import com.highcharts.export.util.*;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/")
public class ExportController extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String FORBIDDEN_WORD = "<!ENTITY";
	private static final Float MAX_WIDTH = 2000.0F;
	private static final Float MAX_SCALE = 4.0F;
	protected static Logger logger = Logger.getLogger("exporter");

	@Autowired
	ServletContext servletContext;

	/* Catch All */
	@RequestMapping(value = "sendToPhysician", method = RequestMethod.POST)
    @ResponseBody
	public String exporter(
            @RequestParam(value = "svg_pain", required = false) String svg_pain,
            @RequestParam(value = "svg_nausea", required = false) String svg_nausea,
            @RequestParam(value = "svg_fatigue", required = false) String svg_fatigue,
            @RequestParam(value = "svg_sleep", required = false) String svg_sleep,
            @RequestParam(value = "svg_constipation", required = false) String svg_constipation,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "filename", required = false) String filename,
            @RequestParam(value = "width", required = false) String width,
            @RequestParam(value = "scale", required = false) String scale,
            @RequestParam(value = "options_pain", required = false) String options_pain,
            @RequestParam(value = "options_nausea", required = false) String options_nausea,
            @RequestParam(value = "options_fatigue", required = false) String options_fatigue,
            @RequestParam(value = "options_sleep", required = false) String options_sleep,
            @RequestParam(value = "options_constipation", required = false) String options_constipation,
            @RequestParam(value = "constr", required = false) String constructor,
            @RequestParam(value = "name", required = false) String patientName,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "dob", required = false) String dob,
            @RequestParam(value = "callback", required = false) String callback)
			throws ServletException, IOException, InterruptedException,
			TimeoutException {

		MimeType mime = getMime(type);
		filename = getFilename(filename);
		Float parsedWidth = widthToFloat(width);
		Float parsedScale = scaleToFloat(scale);

		ByteArrayOutputStream streamPain = new ByteArrayOutputStream();
		ByteArrayOutputStream streamNausea = new ByteArrayOutputStream();
		ByteArrayOutputStream streamFatigue = new ByteArrayOutputStream();
		ByteArrayOutputStream streamSleep = new ByteArrayOutputStream();
		ByteArrayOutputStream streamConstipation = new ByteArrayOutputStream();
        String uuid = UUID.randomUUID().toString();
        String imagePainFileName = uuid.concat("_pain.png");
        String imageNauseaFileName = uuid.concat("_nausea.png");
        String imageFatigueFileName = uuid.concat("_fatigue.png");
        String imageSleepFileName = uuid.concat("_sleep.png");
        String imageConstipationFileName = uuid.concat("_constipation.png");
        OutputStream painFile = new FileOutputStream(imagePainFileName);
        OutputStream nauseaFile = new FileOutputStream(imageNauseaFileName);
        OutputStream fatigueFile = new FileOutputStream(imageFatigueFileName);
        OutputStream sleepFile = new FileOutputStream(imageSleepFileName);
        OutputStream constipationFile = new FileOutputStream(imageConstipationFileName);

		// check if the svg contains a svg-batik exploit.
		if (svg_pain != null
				&& (svg_pain.indexOf(FORBIDDEN_WORD) > -1 || svg_pain
						.indexOf(FORBIDDEN_WORD.toLowerCase()) > -1)) {
			throw new ServletException(
					"The - svg - post parameter could contain a malicious attack");
		}

		if (options_pain != null && !options_pain.isEmpty()) {
			// create a svg file out of the options
			String location = servletContext.getRealPath("/") + "/WEB-INF";

			svg_pain = SVGCreator.getInstance().createSVG(location, options_pain, constructor, callback);
			svg_fatigue = SVGCreator.getInstance().createSVG(location, options_fatigue, constructor, callback);
			svg_nausea = SVGCreator.getInstance().createSVG(location, options_nausea, constructor, callback);
			svg_sleep = SVGCreator.getInstance().createSVG(location, options_sleep, constructor, callback);
			svg_constipation = SVGCreator.getInstance().createSVG(location, options_constipation, constructor, callback);
			if (svg_pain.equals("no-svg")) {
				throw new ServletException(
						"Could not create an SVG out of the options");
			}
		}

		if (svg_pain == null || svg_pain.isEmpty() || svg_pain.equalsIgnoreCase("undefined")) {
			throw new ServletException(
					"The manadatory svg POST parameter is undefined.");
		}

		try {
			ExportController.convert(streamPain, svg_pain, filename, mime, parsedWidth, parsedScale);
			ExportController.convert(streamConstipation, svg_constipation, filename, mime, parsedWidth, parsedScale);
			ExportController.convert(streamFatigue, svg_fatigue, filename, mime, parsedWidth, parsedScale);
			ExportController.convert(streamSleep, svg_sleep, filename, mime, parsedWidth, parsedScale);
			ExportController.convert(streamNausea, svg_nausea, filename, mime, parsedWidth, parsedScale);
            streamPain.writeTo(painFile);
            streamNausea.writeTo(nauseaFile);
            streamFatigue.writeTo(fatigueFile);
            streamConstipation.writeTo(constipationFile);
            streamSleep.writeTo(sleepFile);
            PatientDetails patientDetails = new PatientDetails(patientName, dob, city, state);
            PatientDetailWriter patientDetailWriter = new PatientDetailWriter(patientDetails, uuid);
            String pdfFileName = patientDetailWriter.write();
            MailClient.sendMail(pdfFileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
        return "success";
	}

//	@RequestMapping(value = "/demo", method = RequestMethod.GET)
//	public String demo() {
//		return "demo";
//	}


	@ExceptionHandler(IOException.class)
	public ModelAndView handleIOException(Exception ex) {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("error");
		modelAndView.addObject("message", ex.getMessage());
		return modelAndView;
	}

	@ExceptionHandler(TimeoutException.class)
	public ModelAndView handleTimeoutException(Exception ex) {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("error");
		modelAndView
				.addObject(
						"message",
						"It took too long time to process the options, no SVG is created. Make sure your javascript is correct");
		return modelAndView;
	}

	@ExceptionHandler(InterruptedException.class)
	public ModelAndView handleInterruptedException(Exception ex) {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("error");
		modelAndView
				.addObject(
						"message",
						"It took too long time to process the options, no SVG is created. Make sure your javascript is correct");
		return modelAndView;
	}

	@ExceptionHandler(ServletException.class)
	public ModelAndView handleServletException(Exception ex) {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("error");
		modelAndView.addObject("message", ex.getMessage());
		return modelAndView;
	}

	/*
	 * Util methods
	 */

	public static void convert(ByteArrayOutputStream stream, String svg,
			String filename, MimeType mime, Float width, Float scale)
			throws IOException, ServletException {

		if (!MimeType.SVG.equals(mime)) {
			try {
				stream = SVGRasterizer.getInstance().transcode(stream, svg,
						mime, width, scale);
			} catch (SVGRasterizerException sre) {
				logger.error("Error while transcoding svg file to an image",
						sre);
				stream.close();
				throw new ServletException(
						"Error while transcoding svg file to an image");
			} catch (TranscoderException te) {
				logger.error("Error while transcoding svg file to an image", te);
				stream.close();
				throw new ServletException(
						"Error while transcoding svg file to an image");
			}
		} else {
			stream.write(svg.getBytes());
		}
	}

    private String getFilename(String name) {
		return (name != null) ? name : "chart";
	}

	private static MimeType getMime(String mime) {
		MimeType type = MimeType.get(mime);
		if (type != null) {
			return type;
		}
		return MimeType.PNG;
	}

	private static Float widthToFloat(String width) {
		if (width != null && !width.isEmpty()
				&& !(width.compareToIgnoreCase("undefined") == 0)) {
			Float parsedWidth = Float.valueOf(width);
			if (parsedWidth.compareTo(MAX_WIDTH) > 0) {
				return MAX_WIDTH;
			}
			if (parsedWidth.compareTo(0.0F) > 0) {
				return parsedWidth;
			}
		}
		return null;
	}

	private static Float scaleToFloat(String scale) {
		if (scale != null && !scale.isEmpty()
				&& !(scale.compareToIgnoreCase("undefined") == 0)) {
			Float parsedScale = Float.valueOf(scale);
			if (parsedScale.compareTo(MAX_SCALE) > 0) {
				return MAX_SCALE;
			} else if (parsedScale.compareTo(0.0F) > 0) {
				return parsedScale;
			}
		}
		return null;
	}
}