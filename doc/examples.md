# Examples

### Formatting

Code location on a different line in blue. Line and column number with red background. Log message in reverse colors. Using Selmer's `replace` filter to shorten the file path

```
PWD=/my-app echo '{:mulog/namespace "app.services.logger", :file "/my-app/src/logger.clj", :template "%{?msg}", :column 1,  :line 25, :data {:msg "hi there"  }}

' | edntail --template "{% with-style background blue %}{{file | replace:\"${PWD}/\":\"\"}}:{{line | style: background red}}:{{column | style: background red}}
{% end-with-style %} {{data.msg | style: background white text blue}}


" 
```