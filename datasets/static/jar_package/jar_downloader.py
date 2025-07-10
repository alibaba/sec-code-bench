import argparse
import os
import sys
import urllib.request
from pathlib import Path
from typing import List, Optional, Tuple


def parse_maven_coordinate(coordinate: str) -> Tuple[str, str, str]:
    """Parse Maven coordinate format groupId:artifactId:version"""
    parts = coordinate.split(":")
    if len(parts) != 3:
        raise ValueError(
            f"Invalid Maven coordinate format: {coordinate}, should be groupId:artifactId:version"
        )

    group_id, artifact_id, version = parts
    return group_id, artifact_id, version


def build_maven_url(
    group_id: str,
    artifact_id: str,
    version: str,
    repository_url: str = "https://maven.aliyun.com/repository/public",
) -> str:
    """Build Maven repository download URL"""
    # Replace dots in groupId with slashes
    group_path = group_id.replace(".", "/")

    # Ensure repository_url doesn't end with slash to avoid double slashes
    repo_url = repository_url.rstrip("/")

    # Build URL
    url = f"{repo_url}/{group_path}/{artifact_id}/{version}/{artifact_id}-{version}.jar"
    return url


def download_jar(
    coordinate: str,
    download_dir: str = None,
    repository_url: str = "https://maven.aliyun.com/repository/public",
) -> Tuple[bool, Optional[str]]:
    """Download JAR file for specified Maven coordinate"""
    try:
        # If no download directory specified, use to_resolve directory next to script
        if download_dir is None:
            script_dir = os.path.dirname(os.path.abspath(__file__))
            download_dir = os.path.join(script_dir, "to_resolve")

        # Parse Maven coordinate
        group_id, artifact_id, version = parse_maven_coordinate(coordinate)

        # Build download URL
        jar_url = build_maven_url(group_id, artifact_id, version, repository_url)

        # Ensure download directory exists
        Path(download_dir).mkdir(parents=True, exist_ok=True)

        # Build filename and path
        jar_filename = f"{artifact_id}-{version}.jar"
        jar_path = os.path.join(download_dir, jar_filename)

        # Check if file already exists
        if os.path.exists(jar_path):
            print(f"File {jar_path} already exists, skipping download")
            return True, jar_path

        print(f"Downloading {coordinate}...")
        print(f"URL: {jar_url}")

        # Download file
        urllib.request.urlretrieve(jar_url, jar_path)

        print(f"Download completed: {jar_path}")
        print(f"File size: {os.path.getsize(jar_path)} bytes")

        return True, jar_path

    except Exception as e:
        print(f"Failed to download {coordinate}: {e}")
        return False, None


def download_jars(
    coordinates: List[str],
    download_dir: str = None,
    repository_url: str = "https://maven.aliyun.com/repository/public",
) -> Tuple[List[str], List[str]]:
    """Download multiple JAR files"""
    successful_downloads = []
    failed_downloads = []

    for coordinate in coordinates:
        success, jar_path = download_jar(coordinate, download_dir, repository_url)
        if success:
            successful_downloads.append(jar_path)
        else:
            failed_downloads.append(coordinate)

    return successful_downloads, failed_downloads


def read_dependencies_from_file(config_file: str) -> List[str]:
    """Read dependency list from configuration file"""
    dependencies = []

    if not os.path.exists(config_file):
        print(f"Configuration file {config_file} does not exist")
        return dependencies

    try:
        with open(config_file, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                # Skip empty lines and comments
                if line and not line.startswith("#"):
                    dependencies.append(line)

        print(f"Read {len(dependencies)} dependencies from {config_file}")
        return dependencies

    except Exception as e:
        print(f"Failed to read configuration file: {e}")
        return []


def main():
    """Main function, handle command line arguments"""
    parser = argparse.ArgumentParser(
        description="JAR File Download Tool - Download JAR files from Maven repository",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Usage examples:
  # Download single JAR file
  python jar_downloader.py -c "org.apache.commons:commons-lang3:3.12.0"
  
  # Download multiple JAR files
  python jar_downloader.py -c "org.apache.commons:commons-lang3:3.12.0" "junit:junit:4.13.2"
  
  # Read dependencies from config file and download
  python jar_downloader.py -f dependencies.txt
  
  # Specify download directory
  python jar_downloader.py -c "org.apache.commons:commons-lang3:3.12.0" -d "./jars"
  
  # Use default to_resolve directory
  python jar_downloader.py -c "org.apache.commons:commons-lang3:3.12.0"
  
  # Use different Maven repository
  python jar_downloader.py -c "org.apache.commons:commons-lang3:3.12.0" -r "https://maven.aliyun.com/repository/public"

Configuration file format:
  One Maven coordinate per line, format: groupId:artifactId:version
  Support comments starting with #
  
  Example:
  # Apache Commons
  org.apache.commons:commons-lang3:3.12.0
  org.apache.commons:commons-collections4:4.4
  
  # JUnit
  junit:junit:4.13.2
        """,
    )

    # Add command line arguments
    parser.add_argument(
        "-c",
        "--coordinate",
        "--coordinates",
        nargs="+",
        help="Maven coordinates (format: groupId:artifactId:version)",
    )

    parser.add_argument("-f", "--file", help="Read dependency list from config file")

    parser.add_argument(
        "-d",
        "--dir",
        default=None,
        help="Download directory (default: to_resolve directory next to script)",
    )

    parser.add_argument(
        "-r",
        "--repository",
        default="https://maven.aliyun.com/repository/public",
        help="Maven repository URL (default: https://maven.aliyun.com/repository/public)",
    )

    parser.add_argument(
        "--list-only",
        action="store_true",
        help="Only list JAR files to download, don't perform download",
    )

    args = parser.parse_args()

    # Collect all coordinates
    coordinates = []

    # Get coordinates from command line arguments
    if args.coordinate:
        coordinates.extend(args.coordinate)

    # Read coordinates from configuration file
    config_file = args.file
    if config_file:
        file_coordinates = read_dependencies_from_file(config_file)
        if not file_coordinates:
            print(
                f"Warning: No dependencies read from configuration file {config_file}"
            )
        coordinates.extend(file_coordinates)
    elif not args.coordinate:
        # If no coordinates and no file specified, try to read from default dependencies.txt
        # Look for dependencies.txt in the script directory, not current directory
        script_dir = os.path.dirname(os.path.abspath(__file__))
        default_config = os.path.join(script_dir, "dependencies.txt")
        if os.path.exists(default_config):
            print(
                f"No arguments provided, using default configuration file: {default_config}"
            )
            file_coordinates = read_dependencies_from_file(default_config)
            coordinates.extend(file_coordinates)
        else:
            parser.error(
                f"Must specify -c/--coordinate or -f/--file argument, or have a dependencies.txt file in script directory: {script_dir}"
            )

    if not coordinates:
        print("Error: No JAR file coordinates found to download")
        sys.exit(1)

    # Determine actual download directory
    if args.dir:
        actual_download_dir = args.dir
    else:
        script_dir = os.path.dirname(os.path.abspath(__file__))
        actual_download_dir = os.path.join(script_dir, "to_resolve")

    print(f"Preparing to download {len(coordinates)} JAR files")
    print(f"Download directory: {actual_download_dir}")
    print(f"Maven repository: {args.repository}")
    print("=" * 50)

    # List only files, don't download
    if args.list_only:
        print("List of JAR files to download:")
        for i, coordinate in enumerate(coordinates, 1):
            try:
                group_id, artifact_id, version = parse_maven_coordinate(coordinate)
                jar_url = build_maven_url(
                    group_id, artifact_id, version, args.repository
                )
                jar_filename = f"{artifact_id}-{version}.jar"
                print(f"{i:2d}. {coordinate}")
                print(f"    Filename: {jar_filename}")
                print(f"    URL: {jar_url}")
                print()
            except Exception as e:
                print(f"{i:2d}. {coordinate} - Error: {e}")
        return

    # Execute download
    successful_downloads, failed_downloads = download_jars(
        coordinates, args.dir, args.repository
    )

    # Show results
    print("=" * 50)
    print(f"Download completed!")
    print(f"Successfully downloaded: {len(successful_downloads)}")
    print(f"Failed downloads: {len(failed_downloads)}")

    if successful_downloads:
        print("\nSuccessfully downloaded files:")
        for jar_path in successful_downloads:
            print(f"  ✓ {jar_path}")

    if failed_downloads:
        print("\nFailed download coordinates:")
        for coordinate in failed_downloads:
            print(f"  ✗ {coordinate}")
        sys.exit(1)


if __name__ == "__main__":
    main()
