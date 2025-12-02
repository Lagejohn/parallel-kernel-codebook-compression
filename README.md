# parallel-kernel-codebook-compression
A relatively simple image compression and decompression codec created for academic purposes. Leverages kernel-based vector quantization, huffman coding, and other techniques to achieve better compression than JPEG on grayscale images with extremely minimal loss.

# Execution Instructions
Intended to be built and executed with Java 22; Executable Jar file is packaged and can be executed with:
`java -jar PkcCompressor.jar [input image path] [optional "-m" / "--multithreading" arg]`


If jar file can't be found or otherwise can't be run, code can be compiled from source with normal `javac` commands

Example invocation of jar:

`java -jar PkcCompressor.jar cat256.png -m`

This will initiate the codec from the command line along with verbose logging, and a .pkcc file named [original image name]-compressed.pkcc 
will be generated in the same directory as the source image.

Images can be decompressed/reconstructed with the exact same invocation, except passing in a .pkcc file instead of a normal image.
PKCC will automatically detect that you're trying to decompress instead of compress when it's passed a .pkcc file, reconstructing a PNG by default
(though this can be configured to generate different formats when decompressing by modifiyng the constants defined in PkcCompressor.java)