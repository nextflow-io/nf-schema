package nextflow.validation

import groovy.transform.AutoImplement
import groovy.transform.CompileDynamic

import java.nio.file.AccessMode
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.spi.FileSystemProvider
import java.util.concurrent.ConcurrentHashMap

/**
 * A minimal proxy filesystem for tests. A `fakeproxy://` URI resolves to a registered local
 * target path via toRealPath(), mimicking proxy filesystem plugins like nf-lamin. Discovered
 * by FileSystemProvider.installedProviders() through META-INF/services.
 *
 * @author : rcannood <rcannood@gmail.com>
 */

@AutoImplement(exception = UnsupportedOperationException)
@CompileDynamic
class FakeProxyFileSystemProvider extends FileSystemProvider {

    private static final Map<String, Path> TARGETS = new ConcurrentHashMap<>()

    private final FakeProxyFileSystem fileSystem = new FakeProxyFileSystem(this)

    static void register(String uri, Path target) {
        TARGETS[uri] = target
    }

    static void reset() {
        TARGETS.clear()
    }

    static Path targetFor(URI uri) {
        return TARGETS[uri.toString()]
    }

    @Override
    String getScheme() {
        return 'fakeproxy'
    }

    @Override
    FileSystem getFileSystem(URI uri) {
        return fileSystem
    }

    @Override
    FileSystem newFileSystem(URI uri, Map<String, ?> env) {
        return fileSystem
    }

    @Override
    Path getPath(URI uri) {
        return new FakeProxyPath(uri, fileSystem)
    }

    @Override
    void checkAccess(Path path, AccessMode... modes) throws IOException {
        Path target = targetFor(path.toUri())
        if (target == null || !Files.exists(target)) {
            throw new NoSuchFileException(path.toString())
        }
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        Path target = targetFor(path.toUri())
        if (target == null) {
            throw new NoSuchFileException(path.toString())
        }
        return Files.readAttributes(target, type, options)
    }

}

@AutoImplement(exception = UnsupportedOperationException)
@CompileDynamic
class FakeProxyFileSystem extends FileSystem {

    private final FakeProxyFileSystemProvider fsProvider

    FakeProxyFileSystem(FakeProxyFileSystemProvider fsProvider) {
        this.fsProvider = fsProvider
    }

    @Override
    FileSystemProvider provider() {
        return fsProvider
    }

    @Override
    String getSeparator() {
        return '/'
    }

    @Override
    boolean isOpen() {
        return true
    }

    @Override
    boolean isReadOnly() {
        return true
    }

    @Override
    void close() {
    }

}

@AutoImplement(exception = UnsupportedOperationException)
@CompileDynamic
class FakeProxyPath implements Path {

    private final URI uri
    private final FakeProxyFileSystem fileSystem

    FakeProxyPath(URI uri, FakeProxyFileSystem fileSystem) {
        this.uri = uri
        this.fileSystem = fileSystem
    }

    @Override
    FileSystem getFileSystem() {
        return fileSystem
    }

    @Override
    boolean isAbsolute() {
        return true
    }

    @Override
    URI toUri() {
        return uri
    }

    @Override
    Path toRealPath(LinkOption... options) throws IOException {
        Path target = FakeProxyFileSystemProvider.targetFor(uri)
        if (target == null) {
            throw new NoSuchFileException(uri.toString())
        }
        return target
    }

    @Override
    String toString() {
        return uri.toString()
    }

}
