package au.org.ala.images.upload.service

import au.com.bytecode.opencsv.CSVReader

class CSVService {

    private String _filename

    public CSVService(String filename) {
        _filename = filename
    }

    public void eachLine(Closure eachLine) {
        def file = new File(_filename)
        if (!file.exists()) {
            throw new RuntimeException("File not found! ${_filename}")
        }
        CSVReader reader = new CSVReader(new FileReader(file));
        String [] fields
        String[] columnNames
        int lineNumber = 0
        while ((fields = reader.readNext()) != null) {
            if (lineNumber == 0) {
                columnNames = fields

                // Check that they are actually column names by ensuring the first column is called sourceUrl
                if (!fields || !fields[0] || fields[0] != 'sourceUrl') {
                    throw new RuntimeException("First row does not contain column names, or first column name is not 'sourceUrl'!")
                }
            } else {
                if (eachLine) {
                    def map = [:]
                    for (int i = 0;i < columnNames.length; ++i) {
                        map[columnNames[i]] = fields[i]
                    }
                    eachLine(map, lineNumber)
                }
            }
            lineNumber++
        }
    }
}
