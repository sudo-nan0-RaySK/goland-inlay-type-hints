# GoLand Inlay Type Hints

<div align="center">
  <img src="src/main/resources/META-INF/pluginIcon.svg" alt="Plugin Icon" width="200" height="200">
</div>

![Version](https://img.shields.io/jetbrains/plugin/v/com.horcrux.ssethi.goland-inlay-type-hints)
![Downloads](https://img.shields.io/jetbrains/plugin/d/com.horcrux.ssethi.goland-inlay-type-hints)

A JetBrains GoLand plugin that provides inlay type hints for Go variables, similar to IntelliJ IDEA's type inference
hints for Java.

## Features

- Displays type hints for variables in var declarations
- Shows type information for range clause variables
- Supports complex types including maps and arrays
- Clickable type hints for easy navigation to type definitions

## Installation

1. Open GoLand IDE
2. Go to Settings/Preferences > Plugins
3. Search for "Go - Inlay Type Hints"
4. Click Install
5. Restart GoLand

## Usage

The plugin automatically shows type hints next to variable declarations where types are inferred:

### Basic Variable Declarations

```go
func main() {
    var x = 42        // Shows: x: int
    var y = "hello"   // Shows: y: string
    var z = 3.14      // Shows: z: float64
    
    a := true         // Shows: a: bool
}
```

### Range Clause Variables

```go
func rangeExample() {
    numbers := []int{1, 2, 3, 4, 5}
    
    for i, v := range numbers {
        // Shows: i: int, v: int
        fmt.Println(i, v)
    }
    
    m := map[string]int{"one": 1, "two": 2}
    
    // Shows: k: string, v: int
    for k, v := range m {
        fmt.Println(k, v)
    }
}
```

### Complex Types

```go
func complexTypes() {
    // Maps
    scores := map[string]int{
        "Alice": 95,
        "Bob":   87,
    }
    // Shows: scores: map[string]int
    
    // Arrays and Slices
    matrix := [][]int{
        {1, 2, 3},
        {4, 5, 6},
    }
    // Shows: matrix: [][]int
    
    // Structs
    type Person struct {
        Name string
        Age  int
    }
    
    p := Person{"John", 30}
    // Shows: p: Person
}
```

The type hints are clickable and will navigate to the type definition when clicked.

## Version History

### 1.2.0 (Latest Release)
- Support for range clause variable type hints

### 1.1-SNAPSHOT
- Improved stability and performance
- Support for complex types (maps, arrays, slices, structs)
- Bug fixes and enhancements
- Clickable type hints for navigation to type definitions

### 1.0.0 (Initial Release)
- Initial version of the plugin
- Support for variable type hints in var declarations
