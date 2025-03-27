(defproject artis "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[camel-snake-kebab "0.4.3"]
                 [cheshire "5.13.0"]
                 [ch.qos.logback/logback-classic "1.5.18"]
                 [com.oracle.oci.sdk/oci-java-sdk-common-httpclient-jersey3 "3.61.0"]
                 [com.oracle.oci.sdk/oci-java-sdk-common "3.61.0"]
                 [com.oracle.oci.sdk/oci-java-sdk-identity "3.61.0"]
                 [com.oracle.oci.sdk/oci-java-sdk-usageapi "3.61.0"]
                 [com.oracle.oci.sdk/oci-java-sdk-cloudguard "3.61.0"]
                 [environ "1.2.0"]
                 [org.clojure/clojure "1.12.0"]
                 [org.clojure/data.csv "1.1.0"]
                 [org.clojure/tools.logging "1.3.0"]]
  :main ^:skip-aot artis.core

  :jvm-opts ["-Dclojure.tools.logging.factory=clojure.tools.logging.impl/slf4j-factory"]
  
  :plugins [[environ "1.2.0"]]

  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
