#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset
#set -o xtrace

__dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
__bin="${__dir}/bin"
__extra_libs="${__dir}/extra-libs"

resources=(META-INF/maven/com.google.guava/guava/pom.xml META-INF/services/io.grpc.ServerProvider META-INF/MANIFEST.MF io/grpc/netty/shaded/io/netty/channel/ChannelPromise.class)

echo "Running a classgraph benchmark focused on startup speed"
echo "For more information, see https://github.com/devinrsmith/classgraph-benchmark"
echo

for resource in ${resources[@]}; do
  /usr/bin/time -p "${__bin}/classgraph-benchmark" classloader "${__extra_libs}" "${resource}"
  /usr/bin/time -p "${__bin}/classgraph-benchmark" classgraph "${__extra_libs}" "${resource}"
  echo
done