
import com.microrisc.jlibdpa.JLibDPA;
import com.microrisc.jlibdpa.SimpleJLibDPA;
import com.microrisc.jlibdpa.configuration.DPAConfiguration;
import com.microrisc.jlibdpa.dpaTypes.DPARequest;
import com.microrisc.jlibdpa.dpaTypes.SimpleDPARequest;
import com.microrisc.jlibdpa.timing.NoTimingManager;
import com.microrisc.jlibdpa.timing.TimeManager;
import com.microrisc.jlibiqrf.configuration.IQRFConfiguration;
import com.microrisc.jlibiqrf.iqrfLayer.cdc.CDCConfiguration;

/*
 * Copyright 2016 MICRORISC s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 *
 * @author Martin Strouhal
 */
public class MainTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        IQRFConfiguration iqrfConfig = new CDCConfiguration("COM5");
        TimeManager mng = new NoTimingManager();
        Class<? extends TimeManager> clasz = NoTimingManager.class;
        DPAConfiguration config = new DPAConfiguration(iqrfConfig, NoTimingManager.class);
        JLibDPA dpa = new SimpleJLibDPA(config);
        
        DPARequest request = new SimpleDPARequest(new short[]{0x00, 0x00, 0x07, 0x01, 0xFF, 0xFF});
        dpa.sendAsyncDPARequest(request);
    }
    
}
