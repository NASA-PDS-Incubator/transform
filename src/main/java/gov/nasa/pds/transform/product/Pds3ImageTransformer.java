// Copyright 2006-2014, by the California Institute of Technology.
// ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
// Any commercial use must be negotiated with the Office of Technology Transfer
// at the California Institute of Technology.
//
// This software is subject to U. S. export control laws and regulations
// (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the extent that the software
// is subject to U.S. export control laws and regulations, the recipient has
// the responsibility to obtain export licenses or other export authority as
// may be required before exporting such information to foreign countries or
// providing access to foreign nationals.
//
// $Id$
package gov.nasa.pds.transform.product;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jpl.mipl.io.jConvertIIO;

import gov.nasa.pds.tools.containers.FileReference;
import gov.nasa.pds.tools.label.Label;
import gov.nasa.pds.tools.label.PointerStatement;
import gov.nasa.pds.transform.TransformException;
import gov.nasa.pds.transform.constants.Constants;
import gov.nasa.pds.transform.logging.ToolsLevel;
import gov.nasa.pds.transform.logging.ToolsLogRecord;
import gov.nasa.pds.transform.util.Utility;

/**
 * Class to transform PDS3 images.
 *
 * @author mcayanan
 *
 */
public class Pds3ImageTransformer extends DefaultTransformer {

  /**
   * Constructor to set the flag to overwrite outputs.
   *
   * @param overwrite Set to true to overwrite outputs, false otherwise.
   */
  public Pds3ImageTransformer(boolean overwrite) {
    super(overwrite);
  }

  @Override
  public File transform(File target, File outputDir, String format)
      throws TransformException {
    Label label = null;
    File outputFile = null;
    try {
      label = Utility.parsePds3Label(target);
    } catch (Exception e) {
      throw new TransformException(e.getMessage());
    }
    for (PointerStatement pointer : label.getPointers()) {
      if (pointer.getIdentifier().getId().endsWith("IMAGE") ) {
        File imageFile = null;
        if (!pointer.getFileRefs().isEmpty()) {
          FileReference fileRef = pointer.getFileRefs().get(0);
          imageFile = new File(target.getParent(), fileRef.getPath());
          if (!imageFile.exists()) {
            throw new TransformException("Image file does not exist: "
                + imageFile.toString());
          } else {
            outputFile = Utility.createOutputFile(imageFile, outputDir,
                format);
            if (outputFile.exists() && !overwriteOutput) {
              log.log(new ToolsLogRecord(ToolsLevel.INFO,
                  "Output file already exists. No transformation will occur: "
                  + outputFile.toString(), target));
            } else {
              log.log(new ToolsLogRecord(ToolsLevel.INFO,
                  "Transforming image file: " + imageFile.toString(),
                  target));
              if ("pds".equals(format)) {
                try {
                  String[] args = {imageFile.getCanonicalPath(),
                      outputFile.getCanonicalPath()};
                  if (Constants.EXTERNAL_PROGRAMS.containsKey(format)) {
                    Utility.exec(Constants.EXTERNAL_PROGRAMS.get(format), args);
                  } else {
                    throw new TransformException("Could not find an external "
                        + "program to run for the following format type: "
                        + format);
                  }
                } catch (Exception e) {
                  throw new TransformException(e.getMessage());
                }
                log.log(new ToolsLogRecord(ToolsLevel.INFO,
                    "Transforming label file: " + target, target));
                File pds4Label = Utility.createOutputFile(target, outputDir,
                    "xml");
                try {
                  //Transform the label to PDS4 using the Generate library
                  Utility.generate(target, pds4Label, "vicar-pds3_to_pds4.vm");
                } catch (Exception e) {
                  e.printStackTrace();
                  throw new TransformException("Error occurred while "
                      + "generating PDS4 label: " + e.getMessage());
                }
                log.log(new ToolsLogRecord(ToolsLevel.INFO,
                    "Successfully transformed PDS3 label '" + target
                    + "' to a PDS4 label '" + pds4Label + "'",
                    target));
              } else {
                List<String> args = new ArrayList<String>();
                args.add("inp=" + target.toString());
                args.add("out=" + outputFile.toString());
                if("jp2".equalsIgnoreCase(format)) {
                  args.add("format=jpeg2000");
                } else {
                  args.add("format=" + format);
                }
                args.add("RI");
                args.add("OFORM=BYTE");
                try {
                  jConvertIIO.main(args.toArray(new String[0]));
                } catch (Exception e) {
                  throw new TransformException(e.getMessage());
                }
              }
              log.log(new ToolsLogRecord(ToolsLevel.INFO,
                  "Successfully transformed image file '" + imageFile.toString()
                  + "' to the following output: " + outputFile.toString(),
                  target));
            }
          }
        } else {
          throw new TransformException("No image file references found "
              + "in label.");
        }
      }
    }
    return outputFile;
  }
}
