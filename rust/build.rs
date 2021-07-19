use std::ffi::OsStr;
use std::fs;
use std::io;
use std::path::{Path, PathBuf};

fn find_protos(root: impl AsRef<Path>) -> io::Result<Vec<PathBuf>> {
    let mut protos = vec![];
    for entry in fs::read_dir(root)? {
        let entry = entry?;
        let path = entry.path();
        if entry.file_type()?.is_dir() {
            protos.append(&mut find_protos(path)?);
        } else if path.extension() == Some(OsStr::new("proto")) {
            protos.push(path.to_path_buf());
        }
    }
    Ok(protos)
}

fn main() -> io::Result<()> {
    let include_paths = [
        "../proto/src/main/proto",
        "../external/ibc-go/proto",
        "../external/ibc-go/third_party/proto",
    ];
    include_paths.iter().for_each(|path| {
        println!("cargo:rerun-if-changed={}", path);
    });
    let include_paths: Vec<PathBuf> = include_paths.iter().map(Into::into).collect();
    let mut proto_paths = vec![];
    for path in include_paths.iter() {
        proto_paths.append(&mut find_protos(path)?);
    }
    tonic_build::configure()
        .build_client(true)
        .build_server(false)
        .compile(&proto_paths, &include_paths)
}
