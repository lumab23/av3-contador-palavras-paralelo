package br.com.av3;

import static org.jocl.CL.CL_CONTEXT_PLATFORM;
import static org.jocl.CL.CL_DEVICE_TYPE_GPU;
import static org.jocl.CL.CL_MEM_COPY_HOST_PTR;
import static org.jocl.CL.CL_MEM_READ_ONLY;
import static org.jocl.CL.CL_MEM_WRITE_ONLY;
import static org.jocl.CL.CL_SUCCESS;
import static org.jocl.CL.clBuildProgram;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clCreateCommandQueue;
import static org.jocl.CL.clCreateCommandQueueWithProperties;
import static org.jocl.CL.clCreateContext;
import static org.jocl.CL.clCreateKernel;
import static org.jocl.CL.clCreateProgramWithSource;
import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clEnqueueReadBuffer;
import static org.jocl.CL.clGetDeviceIDs;
import static org.jocl.CL.clGetPlatformIDs;
import static org.jocl.CL.clReleaseCommandQueue;
import static org.jocl.CL.clReleaseContext;
import static org.jocl.CL.clReleaseKernel;
import static org.jocl.CL.clReleaseMemObject;
import static org.jocl.CL.clReleaseProgram;
import static org.jocl.CL.clSetKernelArg;

import java.nio.charset.StandardCharsets;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;
import org.jocl.cl_queue_properties;

public class ParallelGPU {

    private static final String KERNEL_CODE =
            "__kernel void count_word(" +
            "    __global const uchar *letters," +
            "    __global const int *starts," +
            "    __global const int *lengths," +
            "    const int totalWords," +
            "    __global const uchar *target," +
            "    const int targetLength," +
            "    __global int *matches) {" +

            "    int id = get_global_id(0);" +

            "    if (id >= totalWords) {" +
            "        return;" +
            "    }" +

            "    if (lengths[id] != targetLength) {" +
            "        matches[id] = 0;" +
            "        return;" +
            "    }" +

            "    int start = starts[id];" +

            "    for (int i = 0; i < targetLength; i++) {" +
            "        if (letters[start + i] != target[i]) {" +
            "            matches[id] = 0;" +
            "            return;" +
            "        }" +
            "    }" +

            "    matches[id] = 1;" +
            "}";

    public long count(String[] words, String word) {
        if (words.length == 0) {
            return 0;
        }

        String target = WordFile.normalizeWord(word);
        byte[] targetBytes = target.getBytes(StandardCharsets.UTF_8);

        if (targetBytes.length == 0) {
            return 0;
        }

        PreparedWords prepared = prepareWords(words);

        CL.setExceptionsEnabled(false);

        GpuDevice gpu = findGpuDevice();

        if (gpu == null) {
            throw new RuntimeException("Nenhuma GPU com OpenCL foi encontrada.");
        }

        CL.setExceptionsEnabled(true);

        cl_context context = null;
        cl_command_queue queue = null;
        cl_program program = null;
        cl_kernel kernel = null;

        cl_mem lettersBuffer = null;
        cl_mem startsBuffer = null;
        cl_mem lengthsBuffer = null;
        cl_mem targetBuffer = null;
        cl_mem matchesBuffer = null;

        try {
            cl_context_properties properties = new cl_context_properties();
            properties.addProperty(CL_CONTEXT_PLATFORM, gpu.platform);

            context = clCreateContext(
                    properties,
                    1,
                    new cl_device_id[] { gpu.device },
                    null,
                    null,
                    null
            );

            queue = createCommandQueue(context, gpu.device);

            lettersBuffer = clCreateBuffer(
                    context,
                    CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                    Sizeof.cl_char * prepared.letters.length,
                    Pointer.to(prepared.letters),
                    null
            );

            startsBuffer = clCreateBuffer(
                    context,
                    CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                    Sizeof.cl_int * prepared.starts.length,
                    Pointer.to(prepared.starts),
                    null
            );

            lengthsBuffer = clCreateBuffer(
                    context,
                    CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                    Sizeof.cl_int * prepared.lengths.length,
                    Pointer.to(prepared.lengths),
                    null
            );

            targetBuffer = clCreateBuffer(
                    context,
                    CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                    Sizeof.cl_char * targetBytes.length,
                    Pointer.to(targetBytes),
                    null
            );

            int[] matches = new int[words.length];

            matchesBuffer = clCreateBuffer(
                    context,
                    CL_MEM_WRITE_ONLY,
                    Sizeof.cl_int * matches.length,
                    null,
                    null
            );

            program = clCreateProgramWithSource(
                    context,
                    1,
                    new String[] { KERNEL_CODE },
                    null,
                    null
            );

            clBuildProgram(program, 0, null, null, null, null);

            kernel = clCreateKernel(program, "count_word", null);

            int argument = 0;

            clSetKernelArg(kernel, argument++, Sizeof.cl_mem, Pointer.to(lettersBuffer));
            clSetKernelArg(kernel, argument++, Sizeof.cl_mem, Pointer.to(startsBuffer));
            clSetKernelArg(kernel, argument++, Sizeof.cl_mem, Pointer.to(lengthsBuffer));
            clSetKernelArg(kernel, argument++, Sizeof.cl_int, Pointer.to(new int[] { words.length }));
            clSetKernelArg(kernel, argument++, Sizeof.cl_mem, Pointer.to(targetBuffer));
            clSetKernelArg(kernel, argument++, Sizeof.cl_int, Pointer.to(new int[] { targetBytes.length }));
            clSetKernelArg(kernel, argument++, Sizeof.cl_mem, Pointer.to(matchesBuffer));

            long[] globalWorkSize = new long[] { words.length };

            clEnqueueNDRangeKernel(
                    queue,
                    kernel,
                    1,
                    null,
                    globalWorkSize,
                    null,
                    0,
                    null,
                    null
            );

            clEnqueueReadBuffer(
                    queue,
                    matchesBuffer,
                    true,
                    0,
                    Sizeof.cl_int * matches.length,
                    Pointer.to(matches),
                    0,
                    null,
                    null
            );

            long total = 0;

            for (int match : matches) {
                total += match;
            }

            return total;

        } finally {
            if (matchesBuffer != null) {
                clReleaseMemObject(matchesBuffer);
            }

            if (targetBuffer != null) {
                clReleaseMemObject(targetBuffer);
            }

            if (lengthsBuffer != null) {
                clReleaseMemObject(lengthsBuffer);
            }

            if (startsBuffer != null) {
                clReleaseMemObject(startsBuffer);
            }

            if (lettersBuffer != null) {
                clReleaseMemObject(lettersBuffer);
            }

            if (kernel != null) {
                clReleaseKernel(kernel);
            }

            if (program != null) {
                clReleaseProgram(program);
            }

            if (queue != null) {
                clReleaseCommandQueue(queue);
            }

            if (context != null) {
                clReleaseContext(context);
            }
        }
    }

    private cl_command_queue createCommandQueue(cl_context context, cl_device_id device) {
        try {
            cl_queue_properties queueProperties = new cl_queue_properties();
            return clCreateCommandQueueWithProperties(context, device, queueProperties, null);
        } catch (RuntimeException e) {
            if (!isUnsupportedQueueApi(e)) {
                throw e;
            }
        }

        return createCommandQueueLegacy(context, device);
    }

    @SuppressWarnings("deprecation")
    private static cl_command_queue createCommandQueueLegacy(cl_context context, cl_device_id device) {
        return clCreateCommandQueue(context, device, 0L, null);
    }

    private static boolean isUnsupportedQueueApi(RuntimeException e) {
        String message = e.getMessage();
        return message != null && message.contains("clCreateCommandQueueWithProperties");
    }

    private PreparedWords prepareWords(String[] words) {
        byte[][] wordBytes = new byte[words.length][];
        int totalLetters = 0;

        for (int i = 0; i < words.length; i++) {
            wordBytes[i] = words[i].getBytes(StandardCharsets.UTF_8);
            totalLetters += wordBytes[i].length;
        }

        byte[] letters = new byte[totalLetters];
        int[] starts = new int[words.length];
        int[] lengths = new int[words.length];

        int position = 0;

        for (int i = 0; i < words.length; i++) {
            starts[i] = position;
            lengths[i] = wordBytes[i].length;

            System.arraycopy(wordBytes[i], 0, letters, position, wordBytes[i].length);

            position += wordBytes[i].length;
        }

        return new PreparedWords(letters, starts, lengths);
    }

    private GpuDevice findGpuDevice() {
        int[] platformCount = new int[1];
        int status = clGetPlatformIDs(0, null, platformCount);

        if (status != CL_SUCCESS || platformCount[0] == 0) {
            return null;
        }

        cl_platform_id[] platforms = new cl_platform_id[platformCount[0]];
        clGetPlatformIDs(platforms.length, platforms, null);

        for (cl_platform_id platform : platforms) {
            int[] deviceCount = new int[1];

            status = clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, 0, null, deviceCount);

            if (status != CL_SUCCESS || deviceCount[0] == 0) {
                continue;
            }

            cl_device_id[] devices = new cl_device_id[deviceCount[0]];
            status = clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, devices.length, devices, null);

            if (status == CL_SUCCESS && devices.length > 0) {
                return new GpuDevice(platform, devices[0]);
            }
        }

        return null;
    }

    private static class PreparedWords {
        private final byte[] letters;
        private final int[] starts;
        private final int[] lengths;

        private PreparedWords(byte[] letters, int[] starts, int[] lengths) {
            this.letters = letters;
            this.starts = starts;
            this.lengths = lengths;
        }
    }

    private static class GpuDevice {
        private final cl_platform_id platform;
        private final cl_device_id device;

        private GpuDevice(cl_platform_id platform, cl_device_id device) {
            this.platform = platform;
            this.device = device;
        }
    }
}