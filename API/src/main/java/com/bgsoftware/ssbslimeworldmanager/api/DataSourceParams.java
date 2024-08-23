package com.bgsoftware.ssbslimeworldmanager.api;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;

public interface DataSourceParams {

    DataSourceType getType();

    static DataSourceParams parse(ConfigurationSection section) throws IllegalArgumentException {
        DataSourceType dataSourceType = DataSourceType.valueOf(section.getString("type").toUpperCase(Locale.ENGLISH));

        switch (dataSourceType) {
            case API:
                return new API(section.getConfigurationSection("api"));
            case MYSQL:
                return new MySQL(section.getConfigurationSection("mysql"));
            case FILE:
                return new File(section.getConfigurationSection("file"));
        }

        throw new IllegalArgumentException("Cannot handle data source type " + dataSourceType);
    }

    class API implements DataSourceParams {

        public final String username;
        public final String token;
        public final String uri;
        public final boolean ignoreSSLCertificate;

        private API(ConfigurationSection section) {
            this.username = section.getString("username", "");
            this.token = section.getString("token", "");
            this.uri = section.getString("uri", "");
            this.ignoreSSLCertificate = section.getBoolean("ignore-ssl-certificate", false);
        }

        @Override
        public DataSourceType getType() {
            return DataSourceType.API;
        }
    }

    class MySQL implements DataSourceParams {

        public final String url;
        public final String host;
        public final int port;
        public final String username;
        public final String password;
        public final String database;
        public final boolean useSSL;

        private MySQL(ConfigurationSection section) {
            this.url = section.getString("url", "jdbc:mysql://{host}:{port}/{database}?autoReconnect=true&allowMultiQueries=true&useSSL={usessl}");
            this.host = section.getString("host", "127.0.0.1");
            this.port = section.getInt("port", 3306);
            this.username = section.getString("username", "");
            this.password = section.getString("password", "");
            this.database = section.getString("database", "");
            this.useSSL = section.getBoolean("useSSL", false);
        }

        @Override
        public DataSourceType getType() {
            return DataSourceType.MYSQL;
        }
    }

    class File implements DataSourceParams {

        public final String path;

        private File(ConfigurationSection section) {
            this.path = section.getString("path", "slime_worlds");
        }

        @Override
        public DataSourceType getType() {
            return DataSourceType.FILE;
        }
    }

}
