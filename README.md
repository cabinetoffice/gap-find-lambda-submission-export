GAP Lambda submission export

A Spring Boot project that facilitates the background processing of submission exports into a .ODT formatted text document. 
The service matches submission data with any uploaded attachments, creates a zip file and stores that file in an S3 bucket. It then creates a signed URL and emails that URL to the grant admin. 
