# Extended Spring REST shell

# Building and Running


		git clone git@github.com:do4way/rest-shell.git
		cd rest-shell
		./gradlew installApp
		cd build/install/rest-shell-1.2.0.RELEASE
		bin/rest-shell

# Introduction

This project extended the spring HATEOAS-compliant REST shell on the following points. 

+ To support application/x-www-form-urlencoded and multipart/form-data by adding sumit-form command.
+ To support binary data download.
+ To support http request signature using accessId and secretKey.
+ To add http proxy server support.



### Form submission support

#### Usage:

        help submit-form
        Keyword:                   submit-form
        Description:               Issue application/x-www-form-urlencoded data post to create a new resource using json format data.
         Keyword:                  ** default **
         Keyword:                  rel
           Help:                   The path to the resource collections.
           Mandatory:              false
           Default if specified:   '__NULL__'
           Default if unspecified: ''
        
         Keyword:                  follow
           Help:                   If a Location header is returned, immediately follow it.
           Mandatory:              false
           Default if specified:   '__NULL__'
           Default if unspecified: 'false'
        
         Keyword:                  data
           Help:                   The form data to use as the resource.
           Mandatory:              false
           Default if specified:   '__NULL__'
           Default if unspecified: '__NULL__'
        
         Keyword:                  binDataFrom
           Help:                   read attachment data from a file to submit as multipart/form-data
           Mandatory:              false
           Default if specified:   '__NULL__'
           Default if unspecified: '__NULL__'
        
         Keyword:                  output
           Help:                   The path to dump the output to.
           Mandatory:              false
           Default if specified:   '__NULL__'
           Default if unspecified: '__NULL__'
        
        * submit-form - Issue application/x-www-form-urlencoded data post to create a new resource using json format data.

#### Examples:

+ To submit a form data using application/x-www-form-urlencoded format.

        submit-from some/relative/path --data "{name:'value'}"
        
+ To submit a form data with some binary data files.

        sumit-form some/relative/path --data "{name:'value'}" --binDataFrom file/folder/path
        
### binary data download

#### Usage:

        help download
        Keyword:                   download
        Description:               Download binary data.
         Keyword:                  ** default **
         Keyword:                  rel
           Help:                   The path to th resource to download
           Mandatory:              false
           Default if specified:   '__NULL__'
           Default if unspecified: ''
        
         Keyword:                  to
           Help:                   The file path, write the file.
           Mandatory:              true
           Default if specified:   '__NULL__'
           Default if unspecified: '__NULL__'
        
        * download - Download binary data.

#### Example

+ To download a binary file.

        download some/path --to output.jpg
        
### HTTP Request signature

To support request signature authentication. Authentication information that you send in a request must include a signature. To calculate a signature, you set 
the accessId and secretKey with the http accessKey commands.

#### Set the key

You can use the set command to set the key.

        http accessKey set --accessId "your access id" --secretKey "your secret key"

Or you can use global variables to save the accessId and secretKey.

        var set --name accessId --value "your access id"
        var set --name secretKey --value "your secret key"
        http accessKey set
        
#### Reset the key

You can reset the key use the reset command

        http accessKey reset
       
#### Show the current key

        http accessKey show
        
        
### Http proxy server

You can set a proxy server when start the shell.

        JAVA_OPTS="-Dhttp.proxyServer=192.168.1.xxx -Dhttp.proxyPort=8081" build/install/rest-shell-1.2.2.RELEASE/bin/rest-shell

