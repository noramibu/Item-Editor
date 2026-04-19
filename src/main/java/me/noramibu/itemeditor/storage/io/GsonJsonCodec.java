package me.noramibu.itemeditor.storage.io;

import com.google.gson.Gson;

import java.io.Reader;
import java.io.Writer;
import java.util.Objects;

public final class GsonJsonCodec<T> implements AtomicFileUtil.JsonCodec<T> {

    private final Gson gson;
    private final Class<T> type;

    public GsonJsonCodec(Gson gson, Class<T> type) {
        this.gson = Objects.requireNonNull(gson, "gson");
        this.type = Objects.requireNonNull(type, "type");
    }

    @Override
    public T read(Reader reader) {
        return this.gson.fromJson(reader, this.type);
    }

    @Override
    public void write(Writer writer, T value) {
        this.gson.toJson(value, writer);
    }
}

