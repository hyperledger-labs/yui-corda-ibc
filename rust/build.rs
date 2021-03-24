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
        "../external/cosmos-sdk/proto",
        "../external/cosmos-sdk/third_party/proto",
    ];
    let include_paths: Vec<PathBuf> = include_paths.iter().map(Into::into).collect();
    let mut proto_paths = vec![];
    for path in include_paths.iter() {
        proto_paths.append(&mut find_protos(path)?);
    }
    println!("{:?}", include_paths);
    println!("{:?}", proto_paths);
    prost_build::compile_protos(&proto_paths, &include_paths)
}
