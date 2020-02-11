using System;
using System.Threading.Tasks;
using Confluent.Kafka;
using System.Collections.Generic;
using System.Reactive;
using System.Reactive.Linq;
using System.Reactive.Subjects;
using System.Threading;

class DonetProducer
{

    public static async Task Main(string[] args)
    {


        var config = new ProducerConfig { BootstrapServers = "localhost:19092" };

        using (var p = new ProducerBuilder<string, string>(config).Build())
        {
            try
            {
             var random = new Random();
             var list = new List<string>{ "Thorin","Fili","Kili","Balin","Dwalin","Oin","Gloin","Dori","Nori","Ori","Bifur","Bofur","Bombur"};
             int index = random.Next(list.Count);

                var dr = await p.ProduceAsync("villager", new Message<string, string> {Key="donet", Value=list[index] });
                Console.WriteLine($"Donet Delivered '{dr.Value}' to '{dr.TopicPartitionOffset}'");
            }
            catch (ProduceException<Null, string> e)
            {
                Console.WriteLine($"Dotnet Delivery failed: {e.Error.Reason}");
            }
        }
    }


}
