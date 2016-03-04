# nuxeo-webpage-archiver

Given a URL to a web page, the plugin builds a pdf from this webpage.

If the PDF is then saved in a document, you benefit of all the default features of Nuxeo: Full text extraction, Thumbnail, Preview...

* [Operations](#operations)
* [Examples](#examples)
* [Install the Operations in your Studio Project](#install-the-operations-in-your-studio-project)
* [Limitations](#limitations)
* [Build](#build)
* [License](#license)
* [About Nuxeo](#about-nuxeo)


### Operations
The plug-in provides the following operations:

* `Conversion > Webpage to Pdf` (id `WebpageToPdf`)
  * _Input_: `void`
  * _Output_: A blob, the PDF corresponding to the webpage
  * _Parameters_:
    * `url`: The url to use. Full URL with protocol, required
    * `fileName`: The file name for the pdf. Optional. A name built from the host in the URL is provided by default (`"http://my.site.com/more/and/more/page.html"` => `my-site-com.pdf`).
  * This operation runs _synchronously_.

* `Conversion > Webpage to Document` (id `WebpageToDocument`)
  * _Input_: `Document`, the document in which the PDF will be stored
  * _Output_: The `Document` as received in input
  * _Parameters_:
    * `url`: The url to use. Full URL with protocol, required
    * `fileName`: The file name for the pdf. Optional. A name built from the host in the URL is provided by default (`"http://my.site.com/more/and/more/page.html"` => `my-site-com.pdf`).
    * `xpath`: The xpath to use to store the blob. Optional (`file:content` by default)
  * This operation runs **a**_synchronously_, and returns immediately the same document. It does the extraction/PDF-building in an asynchronous worker, and when the conversion is done, it stores the resulting PDF in the `xpath` field and send the `webpageArchived` event (so you can install a listener for this event and be notified once the PDF was generated and stored in the Document)

### Examples

_(see below "Install the Operations in your Studio project")_
    
#### Convert and download from the User Interface, synchronously

Say the URL is stored in the `myinfo:url` field of the current document:

    Fetch > Context Document(s)    
    Conversion > Webpage to Pdf
      url: @{Document["myinfo:url"]}
      fileName
    User Interface > Download File

#### Asynchronously Convert and Save in the Document

Say the URL is stored in the `myinfo:url` field of the current document:

    Fetch > Context Document(s)
    Conversion > Webpage to Document
      url: @{Document["myinfo:url"]}
      fileName
      xpath: file:content

If you want to be notified once the work is done:

1. Add the "webpageArchived" event to the Core Events Registry:
<pre>
{
  "events": {
    "webpageArchived": "webpageArchived"
  }
}
</pre>

2. Create a new Event Handler for this event, and bind it to an automation chain. For example, you could send an email once the pdf is generated.

#### Synchronously Convert and Save in the Document

In previous example, we used the asynchronous `Conversion > Webpage to Document` operation. But we can just use `Conversion > Webpage to PDF` and save the returned PDF in the document. For example:

    Fetch > Context Document(s)
    Push & Pop > Push Document
    Conversion > Webpage to Pdf
      url: @{Document["myinfo:url"]}
      fileName
    Context > Set Context Variable From Input
      name: the PDF
    Push & Pop > Pop Document
    Document > Set File
      file: @{thePDF)
      save: true
      XPath: file:content

### Install the Operations in your Studio Project

To use the operations in your project, you must add their definitions to the "Automation Operations Registry". For example, here is a full registry with the two operations

**WARNING**: If you already have operations in your registry, just copy/past the new one, do not copy the `{"operations": {[` start and `]}}` end).

<pre>
{
  "operations": [
  {
  "id" : "WebpageToDocument",
  "label" : "Webpage to Document",
  "category" : "Conversion",
  "requires" : null,
  "description" : "Read the distant web page and save it as a pdf in the xpath field of input document. This is always an asynchronous operation running in a worker. When it is done, it fires the webpageArchived event. Returns the input document (unchanged)",
  "url" : "WebpageToDocument",
  "signature" : [ "document", "document" ],
  "params" : [ {
    "name" : "url",
    "description" : "",
    "type" : "string",
    "required" : true,
    "widget" : null,
    "order" : 0,
    "values" : [ ]
  }, {
    "name" : "fileName",
    "description" : "",
    "type" : "string",
    "required" : false,
    "widget" : null,
    "order" : 0,
    "values" : [ ]
  }, {
    "name" : "xpath",
    "description" : "",
    "type" : "string",
    "required" : false,
    "widget" : null,
    "order" : 0,
    "values" : [ "file:content" ]
  } ]
}, {
  "id" : "WebpageToPdf",
  "label" : "Webpage to Pdf",
  "category" : "Conversion",
  "requires" : null,
  "description" : "Read the distant web page and save it as a pdf. WARNING: This is a synchronous operation. If the wkhtmltopdf command line locks or takes time, caller may wait.",
  "url" : "WebpageToPdf",
  "signature" : [ "void", "blob" ],
  "params" : [ {
    "name" : "url",
    "description" : "",
    "type" : "string",
    "required" : true,
    "widget" : null,
    "order" : 0,
    "values" : [ ]
  }, {
    "name" : "fileName",
    "description" : "",
    "type" : "string",
    "required" : false,
    "widget" : null,
    "order" : 0,
    "values" : [ ]
  } ]
}
]
}
</pre>

### Limitations

* The plug-in requires the `wkhtmltopdf` command line tool to be installed on your server. Please visit [http://wkhtmltopdf.org](http://wkhtmltopdf.org).
* The plug-in does not handle authentication. The URL must be able to be loaded with no authentication.
* Some webpages can be complicated, with complex css, can contain errors, etc. To avoid the command line to block and, possibly, freeze, it is used with options forcing it to ignore errors (see OSGI-INF/commandLInes.xml), such as
`--load-media-error-handling ignore` and `--load-error-handling ignore</code>`.



### Build

    cd /path/to/nuxeo-webpage-archiver
    mvn clean install    

*NOTE* As of today, unit test are OK in Eclipse, not in Maven. So better run:

    cd /path/to/nuxeo-webpage-archiver
    mvn clean install -DskipTests 


## License
(C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.

All rights reserved. This program and the accompanying materials
are made available under the terms of the GNU Lesser General Public License
(LGPL) version 2.1 which accompanies this distribution, and is available at
http://www.gnu.org/licenses/lgpl-2.1.html

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

Contributors:
Thibaud Arguillere (https://github.com/ThibArg)

## About Nuxeo

Nuxeo provides a modular, extensible Java-based [open source software platform for enterprise content management](http://www.nuxeo.com) and packaged applications for Document Management, Digital Asset Management and Case Management. Designed by developers for developers, the Nuxeo platform offers a modern architecture, a powerful plug-in model and extensive packaging capabilities for building content applications.
