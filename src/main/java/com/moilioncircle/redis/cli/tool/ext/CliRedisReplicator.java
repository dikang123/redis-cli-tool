package com.moilioncircle.redis.cli.tool.ext;

import com.moilioncircle.redis.cli.tool.conf.Configure;
import com.moilioncircle.redis.replicator.CloseListener;
import com.moilioncircle.redis.replicator.Configuration;
import com.moilioncircle.redis.replicator.ExceptionListener;
import com.moilioncircle.redis.replicator.RedisAofReplicator;
import com.moilioncircle.redis.replicator.RedisMixReplicator;
import com.moilioncircle.redis.replicator.RedisRdbReplicator;
import com.moilioncircle.redis.replicator.RedisSocketReplicator;
import com.moilioncircle.redis.replicator.RedisURI;
import com.moilioncircle.redis.replicator.Replicator;
import com.moilioncircle.redis.replicator.Status;
import com.moilioncircle.redis.replicator.cmd.Command;
import com.moilioncircle.redis.replicator.cmd.CommandName;
import com.moilioncircle.redis.replicator.cmd.CommandParser;
import com.moilioncircle.redis.replicator.event.EventListener;
import com.moilioncircle.redis.replicator.io.PeekableInputStream;
import com.moilioncircle.redis.replicator.io.RawByteListener;
import com.moilioncircle.redis.replicator.rdb.RdbVisitor;
import com.moilioncircle.redis.replicator.rdb.datatype.Module;
import com.moilioncircle.redis.replicator.rdb.module.ModuleParser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * @author Baoyi Chen
 */
public class CliRedisReplicator implements Replicator {
    
    protected Replicator replicator;
    
    public CliRedisReplicator(String uri, Configure configure) throws URISyntaxException, IOException {
        Objects.requireNonNull(uri);
        initialize(new RedisURI(uri), configure);
    }
    
    public CliRedisReplicator(RedisURI uri, Configure configure) throws IOException {
        initialize(uri, configure);
    }
    
    private void initialize(RedisURI uri, Configure configure) throws IOException {
        Objects.requireNonNull(uri);
        Objects.requireNonNull(configure);
        Configuration configuration = Configuration.valueOf(uri);
        configuration = configure.merge(configuration);
        if (uri.getFileType() != null) {
            PeekableInputStream in = new PeekableInputStream(uri.toURL().openStream());
            switch (uri.getFileType()) {
                case AOF:
                    if (in.peek() == 'R') {
                        this.replicator = new RedisMixReplicator(in, configuration);
                    } else {
                        this.replicator = new RedisAofReplicator(in, configuration);
                    }
                    break;
                case RDB:
                    this.replicator = new RedisRdbReplicator(in, configuration);
                    break;
                case MIXED:
                    this.replicator = new RedisMixReplicator(in, configuration);
                    break;
                default:
                    throw new UnsupportedOperationException(uri.getFileType().toString());
            }
        } else {
            this.replicator = new RedisSocketReplicator(uri.getHost(), uri.getPort(), configuration);
        }
    }
    
    @Override
    public boolean addRawByteListener(RawByteListener listener) {
        return replicator.addRawByteListener(listener);
    }
    
    @Override
    public boolean removeRawByteListener(RawByteListener listener) {
        return replicator.removeRawByteListener(listener);
    }
    
    @Override
    public void builtInCommandParserRegister() {
        replicator.builtInCommandParserRegister();
    }
    
    @Override
    public CommandParser<? extends Command> getCommandParser(CommandName command) {
        return replicator.getCommandParser(command);
    }
    
    @Override
    public <T extends Command> void addCommandParser(CommandName command, CommandParser<T> parser) {
        replicator.addCommandParser(command, parser);
    }
    
    @Override
    public CommandParser<? extends Command> removeCommandParser(CommandName command) {
        return replicator.removeCommandParser(command);
    }
    
    @Override
    public ModuleParser<? extends Module> getModuleParser(String moduleName, int moduleVersion) {
        return replicator.getModuleParser(moduleName, moduleVersion);
    }
    
    @Override
    public <T extends Module> void addModuleParser(String moduleName, int moduleVersion, ModuleParser<T> parser) {
        replicator.addModuleParser(moduleName, moduleVersion, parser);
    }
    
    @Override
    public ModuleParser<? extends Module> removeModuleParser(String moduleName, int moduleVersion) {
        return replicator.removeModuleParser(moduleName, moduleVersion);
    }
    
    @Override
    public void setRdbVisitor(RdbVisitor rdbVisitor) {
        replicator.setRdbVisitor(rdbVisitor);
    }
    
    @Override
    public RdbVisitor getRdbVisitor() {
        return replicator.getRdbVisitor();
    }
    
    @Override
    public boolean addEventListener(EventListener listener) {
        return replicator.addEventListener(listener);
    }
    
    @Override
    public boolean removeEventListener(EventListener listener) {
        return replicator.removeEventListener(listener);
    }
    
    @Override
    public boolean addCloseListener(CloseListener listener) {
        return replicator.addCloseListener(listener);
    }
    
    @Override
    public boolean removeCloseListener(CloseListener listener) {
        return replicator.removeCloseListener(listener);
    }
    
    @Override
    public boolean addExceptionListener(ExceptionListener listener) {
        return replicator.addExceptionListener(listener);
    }
    
    @Override
    public boolean removeExceptionListener(ExceptionListener listener) {
        return replicator.removeExceptionListener(listener);
    }
    
    @Override
    public boolean verbose() {
        return replicator.verbose();
    }
    
    @Override
    public Status getStatus() {
        return replicator.getStatus();
    }
    
    @Override
    public Configuration getConfiguration() {
        return replicator.getConfiguration();
    }
    
    @Override
    public void open() throws IOException {
        replicator.open();
    }
    
    @Override
    public void close() throws IOException {
        replicator.close();
    }
    
    public static void close(Replicator replicator) {
        if (replicator == null) return;
        try {
            replicator.close();
        } catch (Throwable txt) {
            throw new RuntimeException(txt);
        }
    }
    
    public static void closeQuietly(Replicator replicator) {
        if (replicator == null) return;
        try {
            replicator.close();
        } catch (Throwable t) {
        }
    }
}