package me.noramibu.itemeditor.storage.io;

import com.google.gson.Gson;

import java.io.Reader;
import java.io.Writer;
import java.util.Objects;

public record GsonJsonCodec<T>(Gson gson, Class<T> type) implements AtomicFileUtil.JsonCodec<T> {

    public GsonJsonCodec {
        Objects.requireNonNull(gson, "gson");
        Objects.requireNonNull(type, "type");
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
