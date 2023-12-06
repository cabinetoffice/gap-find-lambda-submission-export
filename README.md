## GAP Lambda submission export

A Spring Boot project that facilitates the background processing of submission exports into a .ODT formatted text document.

The service matches submission data with any uploaded attachments, creates a zip file and stores that file in an S3 bucket. It then adds the S3 object key to the export record in the Apply database, updates the record status to complete and (if all submissions in the queue with the same `exportBatchId` have been processed) sends an email to the user with a link to the exported records.
