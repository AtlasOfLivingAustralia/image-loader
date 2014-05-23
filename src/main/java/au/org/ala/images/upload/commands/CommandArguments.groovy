package au.org.ala.images.upload.commands

import java.lang.reflect.Modifier

class CommandArguments {

    public static databaseFile = new CommandArgument(_switch: "-db", name:'databaseFile', defaultValue: 'images.hsqldb', description: "path to the local database file")
    public static filename = new CommandArgument(_switch: '-f', name:'filename', defaultValue:'images.csv', description:'Path to the CSV file with image urls etc')
    public static serviceBaseUrl = new CommandArgument(_switch: '-s', name: 'serviceBaseUrl', defaultValue: 'http://images.ala.org.au', description: 'Base url to the image service')
    public static statusBatchSize = new CommandArgument(_switch: "-sb", name: "statusBatchSize", defaultValue: 100, description:  "Batch size for status update operations")
    public static uploadBatchSize = new CommandArgument(_switch: "-ub", name: "uploadBatchSize", defaultValue: 10, description:  "Batch size for image upload")
    public static threadCount = new CommandArgument(_switch: '-t', name: 'threadCount', defaultValue: 4, description: 'Base url to the image service')
    public static limit = new CommandArgument(_switch: '-l', name: 'limit', defaultValue: 0, description: 'limit the number of uploads')

    public static CommandArgument findBySwitch(String sw) {

        CommandArgument result = null

        CommandArguments.declaredFields.each { fieldDef ->
            if (Modifier.isPublic(fieldDef.modifiers) && Modifier.isStatic(fieldDef.modifiers)) {
                if (fieldDef.type.isAssignableFrom(CommandArgument)) {
                    def arg = fieldDef.get(null) as CommandArgument
                    if (arg && arg.switch == sw) {
                        result = arg
                    }
                }
            }
        }

        return result

    }

}
