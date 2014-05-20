package au.org.ala.images.upload.commands

class CheckStatusCommand extends AbstractCommand {

    @Override
    String getCommandName() {
        return "checkstatus"
    }

    @Override
    List<CommandArgument> getAcceptedArgs() {
        return [CommandArguments.databaseFile, CommandArguments.serviceBaseUrl, CommandArguments.batchSize]
    }

    @Override
    void executeImpl(Map args) {
        def batch = []
        int count = 0
        int batchSize = args.batchSize;

        imageService.eachImageUrl { imageUrl ->

            batch << [sourceUrl: imageUrl]

            if (++count % batchSize == 0) {

                def imageInfo = webService.getImageInfo(batch)
                batch.each { item ->
                    def status = ""
                    if (imageInfo[item.sourceUrl]) {
                        status = "OK"
                    }
                    item.status = status
                }

                imageService.updateImageStatusBatch(batch)
                batch = []
            }
        }

        if (batch) {
            imageService.updateImageStatusBatch(batch)
        }
    }
}
