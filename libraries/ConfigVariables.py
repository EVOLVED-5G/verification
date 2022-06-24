import configparser

def get_variables(varname, filename):
    config = configparser.ConfigParser()
    config.read(filename)

    variables = {}
    for section in config.sections():
        for key, value in config.items(section):
            var = "%s.%s.%s" % (varname, section, key)
            variables[var] = value
    return variables