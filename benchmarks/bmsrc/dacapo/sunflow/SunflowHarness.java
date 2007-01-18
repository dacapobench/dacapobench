/*
 * This class is directly derived from org.sunflow.Benchmark
 *  
 *  Copyright (c) 2003-2007 Christopher Kulla
 *  
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of
 *  this software and associated documentation files (the "Software"), to deal in the
 *  Software without restriction, including without limitation the rights to use, copy,
 *  modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 *  and to permit persons to whom the Software is furnished to do so, subject to the
 *  following conditions:
 *  
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *  
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 *  INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 *  PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 *  OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 *  SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package dacapo.sunflow;

import java.io.File;

import dacapo.Benchmark;
import dacapo.parser.Config;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import javax.imageio.ImageIO;

import org.sunflow.*;
import org.sunflow.core.Display;
import org.sunflow.core.Tesselatable;
import org.sunflow.core.camera.PinholeLens;
import org.sunflow.core.display.FrameDisplay;
import org.sunflow.core.light.MeshLight;
import org.sunflow.core.primitive.Sphere;
import org.sunflow.core.primitive.TriangleMesh;
import org.sunflow.core.shader.DiffuseShader;
import org.sunflow.core.shader.GlassShader;
import org.sunflow.core.shader.MirrorShader;
import org.sunflow.core.tesselatable.Teapot;
import org.sunflow.image.Color;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.BenchmarkFramework;
import org.sunflow.system.BenchmarkTest;
import org.sunflow.system.UI;
import org.sunflow.system.UserInterface;
import org.sunflow.system.UI.Module;
import org.sunflow.system.UI.PrintLevel;

public class SunflowHarness extends Benchmark {

  public SunflowHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
  }

  protected void prepare() throws Exception {
    super.prepare();
  }

  /**
   */
  public void iterate(String size) throws Exception {
    String[] args = config.getArgs(size);
    new Benchmark(this,args);
  }

  public void cleanup() {
    super.cleanup();
  }

  public static class Benchmark extends SunflowAPI implements BenchmarkTest, UserInterface {
    private PrintStream stream;
    private boolean showGUI;
    private boolean showOutput;
    private boolean showBenchmarkOutput;
    private int errorThreshold;


    public Benchmark(SunflowHarness harness, String[] args) {
      this(harness, System.out, Integer.parseInt(args[0]), Integer.parseInt(args[1]), false, false, false, 6, false, true);
    }

    private Benchmark(SunflowHarness harness, PrintStream stream, int threads, int resolution, boolean showGUI, boolean showOutput,
        boolean showBenchmarkOutput, int errorThreshold, boolean generateMissingReference, boolean checkFrame) {
      this.stream = stream;
      this.showGUI = showGUI;
      this.showOutput = showOutput;
      this.showBenchmarkOutput = showBenchmarkOutput;
      this.errorThreshold = errorThreshold;
      // forward all output through this class (uses the specified stream)
      UI.set(this);
      UI.printInfo(Module.BENCH, "Preparing benchmarking scene ...");
      // settings
      parameter("threads", threads);
      // spawn regular priority threads
      parameter("threads.lowPriority", false);
      parameter("resolutionX", resolution);
      parameter("resolutionY", resolution);
      parameter("aa.min", -1);
      parameter("aa.max", 1);
      parameter("filter", "gaussian");
      parameter("depths.diffuse", 2);
      parameter("depths.reflection", 2);
      parameter("depths.refraction", 2);
      parameter("bucket.order", "hilbert");
      parameter("bucket.size", 32);
      options(SunflowAPI.DEFAULT_OPTIONS);
      // geometry
      buildCornellBox();
      String referenceImageFilename = harness.fileInScratch(String.format("sunflow/golden_%04X.png", resolution));
      boolean generatingReference = false;
      if (new File(referenceImageFilename).exists()) {
        // load the reference image
        UI.printInfo(Module.BENCH, "Loading reference image: %s", referenceImageFilename);
        try {
          BufferedImage bi = ImageIO.read(new File(referenceImageFilename));
          if (bi.getWidth() != resolution || bi.getHeight() != resolution)
            UI.printError(Module.BENCH, "Reference image has invalid resolution! Expected %dx%d found %dx%d", resolution, resolution, bi.getWidth(), bi.getHeight());
          ValidatingDisplay.reference = new int[resolution * resolution];
          for (int y = 0, i = 0; y < resolution; y++)
            for (int x = 0; x < resolution; x++, i++)
              ValidatingDisplay.reference[i] = bi.getRGB(x, resolution - 1 - y); // flip
        } catch (IOException e) {
          UI.printError(Module.BENCH, "Reference image could not be opened");
        }
      } else if (generateMissingReference) {
        UI.printWarning(Module.BENCH, "Reference image was not found - it will be generated to: %s", referenceImageFilename);
        generatingReference = true;
      } else {
        UI.printError(Module.BENCH, "Reference image %s was not found - cannot continue benchmarking", referenceImageFilename);
      }
      // this first render generates the reference frame - it is not timed
      // this also caches the acceleration data structures so it won't be
      // included in the kernel timing
      UI.printInfo(Module.BENCH, "Rendering warmup frame ...");
      render(SunflowAPI.DEFAULT_OPTIONS, checkFrame ? new ValidatingDisplay(generatingReference, errorThreshold) : new FrameDisplay());
      if (!checkFrame)
        return;
      // if the data has been just generated - write it to file for future
      // runs
      if (generatingReference) {
        UI.printInfo(Module.BENCH, "Saving reference image to: %s", referenceImageFilename);
        BufferedImage bi = new BufferedImage(resolution, resolution, BufferedImage.TYPE_INT_RGB);
        for (int y = 0, i = 0; y < resolution; y++)
          for (int x = 0; x < resolution; x++, i++)
            bi.setRGB(x, resolution - 1 - y, ValidatingDisplay.reference[i]);
        try {
          ImageIO.write(bi, "png", new File(referenceImageFilename));
        } catch (IOException e) {
          UI.printError(Module.BENCH, "Unabled to save reference image: %s", e.getMessage());
        }
      }
    }

    private void buildCornellBox() {
      // camera
      parameter("eye", new Point3(0, 0, -600));
      parameter("target", new Point3(0, 0, 0));
      parameter("up", new Vector3(0, 1, 0));
      parameter("fov", 45.0f);
      String name = getUniqueName("camera");
      camera(name, new PinholeLens());
      parameter("camera", name);
      options(SunflowAPI.DEFAULT_OPTIONS);
      // cornell box
      Color gray = new Color(0.70f, 0.70f, 0.70f);
      Color blue = new Color(0.25f, 0.25f, 0.80f);
      Color red = new Color(0.80f, 0.25f, 0.25f);
      Color emit = new Color(15, 15, 15);

      float minX = -200;
      float maxX = 200;
      float minY = -160;
      float maxY = minY + 400;
      float minZ = -250;
      float maxZ = 200;

      float[] verts = new float[] { minX, minY, minZ, maxX, minY, minZ, maxX,
          minY, maxZ, minX, minY, maxZ, minX, maxY, minZ, maxX, maxY,
          minZ, maxX, maxY, maxZ, minX, maxY, maxZ, };
      int[] indices = new int[] { 0, 1, 2, 2, 3, 0, 4, 5, 6, 6, 7, 4, 1, 2,
          5, 5, 6, 2, 2, 3, 6, 6, 7, 3, 0, 3, 4, 4, 7, 3 };

      parameter("diffuse", gray);
      shader("gray_shader", new DiffuseShader());
      parameter("diffuse", red);
      shader("red_shader", new DiffuseShader());
      parameter("diffuse", blue);
      shader("blue_shader", new DiffuseShader());

      // build walls
      parameter("triangles", indices);
      parameter("points", "point", "vertex", verts);
      parameter("faceshaders", new int[] { 0, 0, 0, 0, 1, 1, 0, 0, 2, 2 });
      geometry("walls", new TriangleMesh());

      // instance walls
      parameter("shaders", new String[] { "gray_shader", "red_shader",
      "blue_shader" });
      instance("walls.instance", "walls");

      // create mesh light
      parameter("points", "point", "vertex", new float[] { -50, maxY - 1,
          -50, 50, maxY - 1, -50, 50, maxY - 1, 50, -50, maxY - 1, 50 });
      parameter("triangles", new int[] { 0, 1, 2, 2, 3, 0 });
      parameter("radiance", emit);
      parameter("samples", 8);
      MeshLight light = new MeshLight();
      light.init("light", this);

      // spheres
      parameter("eta", 1.6f);
      shader("Glass", new GlassShader());
      sphere("glass_sphere", "Glass", -120, minY + 55, -150, 50);
      parameter("color", new Color(0.70f, 0.70f, 0.70f));
      shader("Mirror", new MirrorShader());
      sphere("mirror_sphere", "Mirror", 100, minY + 60, -50, 50);

      // scanned model
      geometry("teapot", (Tesselatable) new Teapot());
      parameter("transform", Matrix4.translation(80, -50, 100).multiply(Matrix4.rotateX((float) -Math.PI / 6)).multiply(Matrix4.rotateY((float) Math.PI / 4)).multiply(Matrix4.rotateX((float) -Math.PI / 2).multiply(Matrix4.scale(1.2f))));
      parameter("shaders", "gray_shader");
      instance("teapot.instance1", "teapot");
      parameter("transform", Matrix4.translation(-80, -160, 50).multiply(Matrix4.rotateY((float) Math.PI / 4)).multiply(Matrix4.rotateX((float) -Math.PI / 2).multiply(Matrix4.scale(1.2f))));
      parameter("shaders", "gray_shader");
      instance("teapot.instance2", "teapot");
      // gi options
      parameter("gi.engine", "igi");
      parameter("gi.igi.samples", 90);
      parameter("gi.igi.c", 0.000008f);
      options(DEFAULT_OPTIONS);
    }

    private void sphere(String name, String shaderName, float x, float y, float z, float radius) {
      geometry(name, new Sphere());
      parameter("transform", Matrix4.translation(x, y, z).multiply(Matrix4.scale(radius)));
      parameter("shaders", shaderName);
      instance(name + ".instance", name);
    }

    public void execute() {
      if (showGUI)
        render(SunflowAPI.DEFAULT_OPTIONS, new FrameDisplay());
      // prepare the framework
      BenchmarkFramework framework = new BenchmarkFramework(1, 1200);
      // run the framework
      framework.execute(this);
    }

    public void kernelBegin() {
      // no per loop setup
    }

    public void kernelMain() {
      render(SunflowAPI.DEFAULT_OPTIONS, new ValidatingDisplay(false, errorThreshold));
    }

    public void kernelEnd() {
      // no per loop cleanup
    }

    private static class ValidatingDisplay implements Display {
      private static int[] reference;
      private int[] pixels;
      private int iw, ih;
      private boolean generateReference;
      private int errorThreshold;

      ValidatingDisplay(boolean generateReference, int errorThreshold) {
        this.generateReference = generateReference;
        this.errorThreshold = errorThreshold;
      }

      public void imageBegin(int w, int h, int bucketSize) {
        pixels = new int[w * h];
        iw = w;
        ih = h;
      }

      public void imagePrepare(int x, int y, int w, int h, int id) {
      }

      public void imageUpdate(int x, int y, int w, int h, Color[] data) {
        for (int j = 0, index = 0; j < h; j++) {
          for (int i = 0; i < w; i++, index++) {
            int offset = ((x + i) + iw * (ih - 1 - (y + j)));
            pixels[offset] = data[index].copy().toNonLinear().toRGB();
          }
        }
      }

      public void imageFill(int x, int y, int w, int h, Color c) {
      }

      public void imageEnd() {
        if (generateReference) {
          reference = pixels;
        } else {
          int diff = 0;
          if (reference != null && pixels.length == reference.length) {
            for (int i = 0; i < pixels.length; i++) {
              // count absolute RGB differences
              diff += Math.abs((pixels[i] & 0xFF) - (reference[i] & 0xFF));
              diff += Math.abs(((pixels[i] >> 8) & 0xFF) - ((reference[i] >> 8) & 0xFF));
              diff += Math.abs(((pixels[i] >> 16) & 0xFF) - ((reference[i] >> 16) & 0xFF));
            }
            if (diff > errorThreshold)
              UI.printError(Module.BENCH, "Image check failed! - #errors: %d", diff);
            else
              UI.printInfo(Module.BENCH, "Image check passed!");
          } else
            UI.printError(Module.BENCH, "Image check failed! - reference is not comparable");
        }
      }
    }

    public void print(Module m, PrintLevel level, String s) {
      if (stream != null)
        if (showOutput || (showBenchmarkOutput && m == Module.BENCH))
          stream.println(UI.formatOutput(m, level, s));
      if (level == PrintLevel.ERROR)
        throw new RuntimeException(s);
    }

    public void taskStart(String s, int min, int max) {
    }

    public void taskUpdate(int current) {
    }

    public void taskStop() {
    }
  }


}
