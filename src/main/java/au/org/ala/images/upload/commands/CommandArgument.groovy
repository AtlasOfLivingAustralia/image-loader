package au.org.ala.images.upload.commands

class CommandArgument<T> {

    String _switch
    String name
    String description
    T defaultValue

    public String getSwitch() {
        return _switch
    }

    public void setSwitch(String value) {
        _switch = value
    }

}
