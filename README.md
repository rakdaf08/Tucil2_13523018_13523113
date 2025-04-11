# Image Compression with Quadtree

Image Compression with Quadtree is a technique that compresses an image by recursively subdividing it into smaller blocks. If a block has uniform or similar pixel values, it is stored as a single unit. This method reduces the amount of data needed to represent the image while maintaining its essential structure. Quadtree compression is efficient and can be implemented using brute force or heuristic-based approaches.

# Requirements

User are required to have java installed on their operating system.

# How To Compile

1. Clone the repository
2. Enter the sinppet below
   ```
   javac -d bin src/*.java
   ```
3. You can use the program now

# How To Use

1. Clone the repository
2. Enter the snippet below on the terminal to run the program
   ```
   java -cp bin Main
   ```
3. Enjoy!

# Limitations

The GIF feature cannot be applied to large image files due to the heap memory limitations in Java (see Chapter 6 documentation).

## Author

Raka Daffa Ifitkhaar (13523018) - Kefas Kurnia Jonathan (13523113)
