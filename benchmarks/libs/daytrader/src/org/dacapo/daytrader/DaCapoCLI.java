package org.dacapo.daytrader;

import org.apache.geronimo.cli.AbstractCLI;
import org.apache.geronimo.cli.client.ClientCLParser;
import org.apache.geronimo.cli.CLParser;
import org.apache.geronimo.kernel.util.MainConfigurationBootstrapper;


/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
public class DaCapoCLI extends AbstractCLI {
    MainConfigurationBootstrapper mainBS = null;
    
    public static void main(String[] args) {
        DaCapoCLI cli = new DaCapoCLI(args);
        cli.executeMain();
        cli.shutdown();
    }

    protected DaCapoCLI(String[] args) {
        super(args, System.err);
    }
    
    @Override
    protected CLParser getCLParser() {
        return new ClientCLParser(System.out);
    }
    
    private void shutdown() {
      if (mainBS != null) {
        mainBS.getKernel().shutdown();
      }
    }

    @Override
    protected MainConfigurationBootstrapper newMainConfigurationBootstrapper() {
      mainBS = new MainConfigurationBootstrapper();
      return mainBS;
    }
}
